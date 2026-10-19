package com.bloxbean.cardano.yaci.store.snapshot.load;

import com.bloxbean.cardano.yaci.store.snapshot.util.Identifiers;
import com.bloxbean.cardano.yaci.store.snapshot.util.ParallelWorkers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Drops the keys and indexes of the tables being loaded and rebuilds them after the load.
 *
 * <p>Loading into indexed tables makes every row an index insert, and a key on a hash is a random
 * one: on mainnet the load spends most of its time writing index pages to the WAL. Building an index
 * once over loaded data is a single sorted pass.
 *
 * <p>Nothing here lists which indexes a table has. They are read from the catalog at the start of
 * the load, so whatever the migrations created is exactly what comes back. Indexes applied later by
 * {@code apply-indexes} are not present yet and are not touched; if an operator applied them before
 * the import, they are captured and rebuilt the same way.
 *
 * <p>The captured definitions live in the import journal, written before anything is dropped. A
 * resumed import reads them from there, never from the half-dropped catalog. An entry is dropped
 * idempotently and rebuilt in the same transaction that marks it built, so a crash at any point
 * leaves a state the next run completes. Keys are rebuilt before a table's other indexes, so
 * duplicate rows fail on the key, naming it.
 */
public class DeferredIndexes {

    private static final Logger log = LoggerFactory.getLogger(DeferredIndexes.class);

    public static final String TABLE = "_yaci_snapshot_import_index";
    static final String CONSTRAINT = "CONSTRAINT";
    static final String INDEX = "INDEX";
    static final long PROGRESS_MILLIS = 60_000;
    /** Parallel workers asked for by the first build, at most half the server's pool. */
    static final int LEAD_PARALLEL_WORKERS = 4;

    /** Postgres prints an index on a partitioned table as {@code ON ONLY}, which builds no partitions. */
    private static final Pattern ON_ONLY =
            Pattern.compile("^(CREATE (?:UNIQUE )?INDEX (?:\"(?:[^\"]|\"\")*\"|\\S+) ON )ONLY ");

    /** An index definition's uniqueness and everything from {@code USING}, without its name and table. */
    private static final String IDENTIFIER = "(?:\"(?:[^\"]|\"\")*\"|[^\\s.\"]+)";
    private static final Pattern INDEX_BODY = Pattern.compile("^CREATE (UNIQUE )?INDEX " + IDENTIFIER
            + " ON (?:ONLY )?(?:" + IDENTIFIER + "\\.)?" + IDENTIFIER + " (USING .+)$", Pattern.DOTALL);

    /**
     * One captured key or index.
     *
     * @param definition {@code pg_get_constraintdef} or {@code pg_get_indexdef}, compared verbatim
     *                   after the rebuild
     * @param key        a primary key, unique or exclusion constraint, or a unique index
     * @param keyColumns primary key columns in attribute order, as {@link PgSchema#primaryKey} reports
     *                   them; empty for anything else
     */
    public record Entry(String table, String name, String kind, boolean key, String definition,
                        List<String> keyColumns, boolean built) {

        String createSql(String schema) {
            if (CONSTRAINT.equals(kind)) {
                return "ALTER TABLE " + Identifiers.quote(schema) + "." + Identifiers.quote(table)
                        + " ADD CONSTRAINT " + Identifiers.quote(name) + " " + definition;
            }
            return ON_ONLY.matcher(definition).replaceFirst("$1");
        }

        String dropSql(String schema) {
            if (CONSTRAINT.equals(kind)) {
                return "ALTER TABLE " + Identifiers.quote(schema) + "." + Identifiers.quote(table)
                        + " DROP CONSTRAINT IF EXISTS " + Identifiers.quote(name);
            }
            return "DROP INDEX IF EXISTS " + Identifiers.quote(schema) + "." + Identifiers.quote(name);
        }

        String label() {
            return (key ? (CONSTRAINT.equals(kind) ? "key " : "unique index ") : "index ") + name + " on " + table;
        }

        /**
         * This entry's copy on one partition of its table, unnamed so Postgres picks the name a
         * build on the parent gives it; null when it cannot be built that way.
         */
        String partitionSql(String schema, String partition) {
            String target = Identifiers.quote(schema) + "." + Identifiers.quote(partition);
            if (CONSTRAINT.equals(kind)) {
                return definition.startsWith("PRIMARY KEY ") || definition.startsWith("UNIQUE ")
                        ? "ALTER TABLE " + target + " ADD " + definition : null;
            }
            Matcher m = INDEX_BODY.matcher(definition);
            return m.matches() ? "CREATE " + (m.group(1) == null ? "" : m.group(1)) + "INDEX ON " + target + " "
                    + m.group(2) : null;
        }
    }

    /** {@code UNIQUE } or empty, then everything from {@code USING}; null when it is not an index definition. */
    static String indexBody(String definition) {
        Matcher m = INDEX_BODY.matcher(definition);
        return m.matches() ? (m.group(1) == null ? "" : m.group(1)) + m.group(2) : null;
    }

    /**
     * One unit of work for a rebuild connection: {@code entries} built in order on {@code table}, or,
     * with a {@code partition}, each entry's copy built on that partition alone. It starts once every
     * latch in {@code after} is open and counts down {@code done} whether it ran, failed or was dropped.
     *
     * @param weight how long it is expected to take, compared with other tasks: the bytes it reads
     * @param order  position in the order the tasks were planned, which breaks ties
     */
    record Task(String table, String partition, List<Entry> entries,
                List<CountDownLatch> after, List<CountDownLatch> done, long weight, int order) {
        String label() {
            return partition == null ? entries.get(0).label() : entries.get(0).label() + ", partition " + partition;
        }

        boolean ready() {
            return after.stream().allMatch(l -> l.getCount() == 0);
        }
    }

    /**
     * Hands each free connection the heaviest task whose prerequisites are done.
     *
     * <p>A fixed queue made a connection wait on a task it had taken but could not start, and kept
     * long builds behind short ones: on mainnet the transaction indexes waited behind 1,365 reward
     * partition copies, then ran with six connections idle. Here a task is only handed out once it can
     * start, and the longest first, so the short ones fill the gaps.
     */
    static final class Scheduler {
        private final java.util.PriorityQueue<Task> ready = new java.util.PriorityQueue<>(
                Comparator.comparingLong(Task::weight).reversed().thenComparingInt(Task::order));
        private final List<Task> waiting = new ArrayList<>();
        private int running;
        private boolean stopped;

        Scheduler(List<Task> tasks) {
            tasks.forEach(t -> (t.ready() ? ready : waiting).add(t));
        }

        /** The next task to run, waiting for one to become ready; null once there is nothing left to run. */
        synchronized Task take() throws InterruptedException {
            while (!stopped) {
                Task task = ready.poll();
                if (task != null) {
                    running++;
                    return task;
                }
                if (running == 0) {
                    if (!waiting.isEmpty()) {
                        throw new IllegalStateException(waiting.size() + " rebuild task(s) wait on tasks that"
                                + " will never run, starting with " + waiting.get(0).label());
                    }
                    return null;
                }
                wait();
            }
            return null;
        }

        /** The heaviest ready task, without taking it. */
        synchronized Task peek() {
            return ready.peek();
        }

        /** Marks {@code task} done, whether it succeeded or not, and readies what waited on it. */
        synchronized void finished(Task task) {
            task.done().forEach(CountDownLatch::countDown);
            running--;
            for (java.util.Iterator<Task> it = waiting.iterator(); it.hasNext(); ) {
                Task t = it.next();
                if (t.ready()) {
                    it.remove();
                    ready.add(t);
                }
            }
            notifyAll();
        }

        /** Hands out nothing more, after a failure; the tasks running now still finish. */
        synchronized void stop() {
            stopped = true;
            ready.forEach(t -> t.done().forEach(CountDownLatch::countDown));
            waiting.forEach(t -> t.done().forEach(CountDownLatch::countDown));
            ready.clear();
            waiting.clear();
            notifyAll();
        }
    }

    /** Opens a connection for one rebuild worker. */
    @FunctionalInterface
    public interface ConnectionFactory {
        Connection open() throws SQLException;
    }

    private final Connection conn;
    private final String schema;

    public DeferredIndexes(Connection conn, String schema) {
        this.conn = conn;
        this.schema = Identifiers.requireSqlIdentifier(schema, "target schema");
    }

    private String q() {
        return Identifiers.quote(schema) + "." + Identifiers.quote(TABLE);
    }

    public void createIfAbsent() throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS " + q() + " ("
                    + " snapshot_id varchar(64) NOT NULL,"
                    + " table_name varchar(100) NOT NULL,"
                    + " name varchar(100) NOT NULL,"
                    + " kind varchar(20) NOT NULL,"
                    + " is_key boolean NOT NULL,"
                    + " definition text NOT NULL,"
                    + " key_columns text,"
                    + " built boolean NOT NULL DEFAULT false,"
                    + " built_at timestamp,"
                    + " PRIMARY KEY (snapshot_id, kind, table_name, name))");
        }
    }

    public boolean exists() throws SQLException {
        return exists(conn, schema);
    }

    private static boolean exists(Connection conn, String schema) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM pg_class c"
                + " JOIN pg_namespace n ON n.oid = c.relnamespace WHERE n.nspname = ? AND c.relname = ?")) {
            ps.setString(1, schema);
            ps.setString(2, TABLE);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Captures the keys and indexes of {@code tables} (and their partitions) unless this snapshot's
     * entries are already recorded, then drops every entry not yet rebuilt.
     *
     * @return the entries still to rebuild
     */
    public List<Entry> defer(String snapshotId, Collection<String> tables, Consumer<String> say)
            throws SQLException {
        createIfAbsent();
        List<Entry> entries = entries(snapshotId);
        if (entries.isEmpty()) {
            Set<String> excluded = referencedByForeignKeys(tables);
            if (!excluded.isEmpty()) {
                say.accept("Keeping indexes on " + excluded + ": foreign keys refer to them");
            }
            List<String> deferrable = tables.stream().filter(t -> !excluded.contains(t)).distinct().toList();
            entries = capture(deferrable);
            record(snapshotId, entries);
        }
        List<Entry> pending = entries.stream().filter(e -> !e.built()).toList();
        if (pending.isEmpty()) {
            return pending;
        }
        // One transaction per table: dropping an index on a partitioned table locks every partition,
        // and a single transaction over all of them can exhaust the lock table.
        Map<String, List<Entry>> byTable = new LinkedHashMap<>();
        pending.forEach(e -> byTable.computeIfAbsent(e.table(), k -> new ArrayList<>()).add(e));
        boolean autoCommit = conn.getAutoCommit();
        try {
            conn.setAutoCommit(false);
            for (List<Entry> group : byTable.values()) {
                try (Statement st = conn.createStatement()) {
                    // Plain indexes first; a constraint's index goes with the constraint.
                    for (Entry e : group.stream().sorted(Comparator.comparing(Entry::key)).toList()) {
                        st.execute(e.dropSql(schema));
                    }
                }
                conn.commit();
            }
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(autoCommit);
        }
        long keys = pending.stream().filter(Entry::key).count();
        say.accept(String.format("Deferred %d key(s) and %d index(es) on %d table(s) until the load completes",
                keys, pending.size() - keys, byTable.size()));
        return pending;
    }

    /**
     * Rebuilds every entry not yet built on up to {@code workers} connections: each table's keys before
     * its other indexes, and of the builds that can start, the one that reads the most first.
     *
     * <p>Postgres hands out parallel workers when a build starts, from a pool shared by every build.
     * The first task, the largest table's key and the longest build, starts alone with up to
     * {@link #LEAD_PARALLEL_WORKERS}; the other connections start once its workers are running.
     * Started together, the largest key could find the pool empty and scan its table in one process.
     *
     * @param maintenanceWorkMem per-connection {@code maintenance_work_mem}, e.g. {@code "3GB"}, or null
     *                           for the server default
     */
    public void rebuild(String snapshotId, int workers, String maintenanceWorkMem,
                        ConnectionFactory connections, Consumer<String> say) throws SQLException {
        List<Entry> pending = entries(snapshotId).stream().filter(e -> !e.built()).toList();
        if (pending.isEmpty()) {
            return;
        }
        // Size on disk, not rows: address_utxo has fewer rows than epoch_stake but takes far longer.
        Map<String, Long> rows = new HashMap<>();
        Map<String, String> parents = partitionParents(pending.stream().map(Entry::table).distinct().toList(), null);
        Map<String, Long> bytes = tableBytes(pending.stream()
                .map(e -> parents.getOrDefault(e.table(), e.table())).distinct().toList());
        for (Entry e : pending) {
            rows.put(e.table(), bytes.getOrDefault(parents.getOrDefault(e.table(), e.table()), 0L));
        }
        Comparator<String> largestFirst = Comparator.comparingLong((String t) -> rows.get(t)).reversed()
                .thenComparing(Comparator.naturalOrder());

        // Keys of a table are rebuilt by one task, in order; a table's other indexes wait for them.
        // On a partitioned table, each partition's copy is its own task and the parent's build, which
        // waits for them, only attaches them: Postgres builds a parent's partitions one at a time.
        Map<String, Long> partitionBytes = new HashMap<>();
        Map<String, List<String>> partitions = leafPartitions(new ArrayList<>(rows.keySet()), partitionBytes);
        Map<String, List<Entry>> keysByTable = new java.util.TreeMap<>(largestFirst);
        pending.stream().filter(Entry::key)
                .forEach(e -> keysByTable.computeIfAbsent(e.table(), k -> new ArrayList<>()).add(e));
        Map<String, CountDownLatch> keysDone = new HashMap<>();
        keysByTable.keySet().forEach(t -> keysDone.put(t, new CountDownLatch(1)));
        Set<String> indexed = new java.util.HashSet<>();
        pending.stream().filter(e -> !e.key()).forEach(e -> indexed.add(e.table()));

        List<Task> tasks = new ArrayList<>();
        Map<String, Integer> copies = new HashMap<>();
        // A key goes ahead of an index build of the same size: the table's indexes wait for it.
        keysByTable.forEach((table, keys) -> addTasks(tasks, table, keys, List.of(), keysDone.get(table),
                partitions.get(table), rows.get(table), partitionBytes, indexed.contains(table) ? 2 : 1, copies));
        pending.stream().filter(e -> !e.key())
                .sorted(Comparator.comparing(Entry::table, largestFirst).thenComparing(Entry::name))
                .forEach(e -> addTasks(tasks, e.table(), List.of(e),
                        keysDone.containsKey(e.table()) ? List.of(keysDone.get(e.table())) : List.of(), null,
                        partitions.get(e.table()), rows.get(e.table()), partitionBytes, 1, copies));
        Scheduler scheduler = new Scheduler(tasks);

        int threads = Math.max(1, Math.min(workers, tasks.size()));
        say.accept(String.format("Rebuilding %d key(s) and index(es) on %d table(s) with %d connection(s)%s",
                pending.size(), rows.size(), threads,
                maintenanceWorkMem == null ? "" : ", maintenance_work_mem " + maintenanceWorkMem));
        int leadWorkers = threads > 1 && scheduler.peek() != null ? leadParallelWorkers() : 0;
        if (leadWorkers > 0) {
            say.accept(String.format("Starting %s first, with up to %d parallel workers", scheduler.peek().label(),
                    leadWorkers));
        }
        java.util.concurrent.atomic.AtomicInteger leadPid = new java.util.concurrent.atomic.AtomicInteger();
        CountDownLatch leadFirstTaskDone = new CountDownLatch(1);

        List<SQLException> failures = new ArrayList<>();
        Set<String> failedTables = java.util.concurrent.ConcurrentHashMap.newKeySet();
        Map<String, Long> firstCopyStarted = new java.util.concurrent.ConcurrentHashMap<>();
        java.util.concurrent.atomic.AtomicInteger done = new java.util.concurrent.atomic.AtomicInteger();
        long started = System.currentTimeMillis();
        var timer = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(
                Thread.ofPlatform().name("snapshot-index-progress").daemon().factory());
        timer.scheduleAtFixedRate(() -> {
            String line = inFlight();
            if (line != null) {
                synchronized (say) {
                    say.accept(String.format("%d/%d done, elapsed %s; %s", done.get(), pending.size(),
                            LoadProgress.duration(System.currentTimeMillis() - started), line));
                }
            }
        }, PROGRESS_MILLIS, PROGRESS_MILLIS, java.util.concurrent.TimeUnit.MILLISECONDS);
        List<Thread> pool = new ArrayList<>();
        for (int w = 0; w < threads; w++) {
            boolean lead = w == 0 && leadWorkers > 0;
            Thread thread = Thread.ofPlatform().name("snapshot-index-" + w).unstarted(() -> {
                try (Connection c = connections.open()) {
                    c.setAutoCommit(false);
                    try (Statement st = c.createStatement()) {
                        if (maintenanceWorkMem != null) {
                            st.execute("SET maintenance_work_mem = '" + maintenanceWorkMem + "'");
                        }
                        if (lead) {
                            st.execute("SET max_parallel_maintenance_workers = " + leadWorkers);
                            leadPid.set(ParallelWorkers.backendPid(c));
                        }
                    }
                    c.commit();
                    Task task;
                    while ((task = scheduler.take()) != null) {
                        try {
                            if (!failedTables.contains(task.table())) {
                                run(c, snapshotId, task, copies, firstCopyStarted, done, pending.size(), say);
                            }
                        } catch (SQLException e) {
                            failedTables.add(task.table());
                            synchronized (failures) {
                                failures.add(e);
                            }
                            scheduler.stop();
                        } finally {
                            scheduler.finished(task);
                        }
                        if (lead && leadFirstTaskDone.getCount() > 0) {
                            try (Statement st = c.createStatement()) {
                                st.execute("RESET max_parallel_maintenance_workers");
                            }
                            c.commit();
                            leadFirstTaskDone.countDown();
                        }
                    }
                } catch (SQLException | RuntimeException e) {
                    synchronized (failures) {
                        failures.add(e instanceof SQLException sql ? sql : new SQLException(e.getMessage(), e));
                    }
                    scheduler.stop();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    leadFirstTaskDone.countDown();
                }
            });
            pool.add(thread);
            thread.start();
            if (lead) {
                ParallelWorkers.awaitLaunched(conn, leadPid, leadFirstTaskDone);
            }
        }
        try {
            for (Thread t : pool) {
                t.join();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Index rebuild interrupted", e);
        } finally {
            timer.shutdownNow();
        }
        if (!failures.isEmpty()) {
            throw failures.get(0);
        }
        say.accept(String.format("Rebuilt %d key(s) and index(es) in %s", pending.size(),
                LoadProgress.duration(System.currentTimeMillis() - started)));
    }

    /**
     * The builds running now, from {@code pg_stat_progress_create_index}: one table's key can take many
     * minutes, and without this the rebuild is silent until it finishes. Null when nothing is running
     * or the view cannot be read.
     */
    private String inFlight() {
        String sql = """
                SELECT t.relname, p.phase, p.blocks_done, p.blocks_total, p.tuples_done, p.tuples_total,
                       p.partitions_done, p.partitions_total
                FROM pg_stat_progress_create_index p
                JOIN pg_class t ON t.oid = p.relid
                JOIN pg_namespace n ON n.oid = t.relnamespace
                WHERE n.nspname = ?
                ORDER BY t.relname
                """;
        List<String> out = new ArrayList<>();
        synchronized (conn) {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, schema);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        long blocksTotal = rs.getLong(4);
                        long tuplesTotal = rs.getLong(6);
                        String pct = blocksTotal > 0 ? String.format(" %d%%", rs.getLong(3) * 100 / blocksTotal)
                                : tuplesTotal > 0 ? String.format(" %d%%", rs.getLong(5) * 100 / tuplesTotal) : "";
                        String parts = rs.getLong(8) > 0
                                ? String.format(" (partition %d/%d)", rs.getLong(7), rs.getLong(8)) : "";
                        out.add(rs.getString(1) + ": " + rs.getString(2) + pct + parts);
                    }
                }
            } catch (SQLException e) {
                log.debug("Unable to read index build progress", e);
                return null;
            }
        }
        return out.isEmpty() ? null : "building " + String.join("; ", out);
    }

    /**
     * Parallel workers for the first build: {@link #LEAD_PARALLEL_WORKERS}, at most half the pool, or
     * 0 when that is no more than the server already gives every build.
     */
    private int leadParallelWorkers() {
        String sql = "SELECT least(current_setting('max_parallel_workers')::int,"
                + " current_setting('max_worker_processes')::int),"
                + " current_setting('max_parallel_maintenance_workers')::int";
        synchronized (conn) {
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
                rs.next();
                int lead = Math.min(LEAD_PARALLEL_WORKERS, rs.getInt(1) / 2);
                return lead > rs.getInt(2) ? lead : 0;
            } catch (SQLException e) {
                log.debug("Unable to read the parallel worker settings", e);
                return 0;
            }
        }
    }

    /** Heap size in bytes of each table, its partitions included. */
    private Map<String, Long> tableBytes(List<String> tables) throws SQLException {
        Map<String, Long> out = new HashMap<>();
        String sql = """
                SELECT c.relname, coalesce((SELECT sum(pg_relation_size(t.relid)) FROM pg_partition_tree(c.oid) t),
                                           pg_relation_size(c.oid))
                FROM pg_class c
                JOIN pg_namespace n ON n.oid = c.relnamespace
                WHERE n.nspname = ? AND c.relname = ANY (?::text[])
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, schema);
            ps.setArray(2, conn.createArrayOf("text", tables.toArray()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.put(rs.getString(1), rs.getLong(2));
                }
            }
        }
        return out;
    }

    /**
     * Adds the tasks that build {@code entries} on {@code table}: one task, or, when every entry can be
     * built partition by partition, one per partition and then the parent's, which attaches them.
     */
    private void addTasks(List<Task> tasks, String table, List<Entry> entries, List<CountDownLatch> after,
                          CountDownLatch done, List<String> partitions, long tableBytes,
                          Map<String, Long> partitionBytes, int factor, Map<String, Integer> copies) {
        List<CountDownLatch> finished = done == null ? List.of() : List.of(done);
        if (partitions == null || entries.stream().anyMatch(e -> e.partitionSql(schema, partitions.get(0)) == null)) {
            tasks.add(new Task(table, null, entries, after, finished, tableBytes * factor, tasks.size()));
            return;
        }
        CountDownLatch built = new CountDownLatch(partitions.size());
        partitions.forEach(p -> tasks.add(new Task(table, p, entries, after, List.of(built),
                partitionBytes.getOrDefault(p, 0L) * factor, tasks.size())));
        List<CountDownLatch> parentAfter = new ArrayList<>(after);
        parentAfter.add(built);
        // Only attaches the partitions' copies, and what waits on the table waits on it.
        tasks.add(new Task(table, null, entries, parentAfter, finished, Long.MAX_VALUE, tasks.size()));
        entries.forEach(e -> copies.put(entryKey(e), partitions.size()));
    }

    private static String entryKey(Entry e) {
        return e.kind() + " " + e.table() + " " + e.name();
    }

    private void run(Connection c, String snapshotId, Task task, Map<String, Integer> copies,
                     Map<String, Long> firstCopyStarted, java.util.concurrent.atomic.AtomicInteger done, int total,
                     Consumer<String> say) throws SQLException {
        for (Entry e : task.entries()) {
            long t0 = System.currentTimeMillis();
            if (task.partition() != null) {
                firstCopyStarted.putIfAbsent(entryKey(e), t0);
                buildOnPartition(c, e, task.partition());
                continue;
            }
            build(c, snapshotId, e);
            Integer partitions = copies.get(entryKey(e));
            String line = String.format("%d/%d %s%s (%s)", done.incrementAndGet(), total, e.label(),
                    partitions == null ? "" : ", " + partitions + " partitions in parallel",
                    LoadProgress.duration(System.currentTimeMillis() - firstCopyStarted.getOrDefault(entryKey(e), t0)));
            synchronized (say) {
                say.accept(line);
            }
        }
    }

    /**
     * Builds an entry's copy on one partition, unless a copy from an interrupted run is already there:
     * the parent's build attaches whichever matching index a partition has.
     */
    private void buildOnPartition(Connection c, Entry e, String partition) throws SQLException {
        try (Statement st = c.createStatement()) {
            if (!hasCopy(c, e, partition)) {
                st.execute(e.partitionSql(schema, partition));
            }
            c.commit();
        } catch (SQLException ex) {
            c.rollback();
            throw failure(e, ex);
        }
    }

    private boolean hasCopy(Connection c, Entry e, String partition) throws SQLException {
        String relation = Identifiers.quote(schema) + "." + Identifiers.quote(partition);
        if (CONSTRAINT.equals(e.kind())) {
            try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM pg_constraint"
                    + " WHERE conrelid = ?::regclass AND pg_get_constraintdef(oid) = ?")) {
                ps.setString(1, relation);
                ps.setString(2, e.definition());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        }
        String body = indexBody(e.definition());
        try (PreparedStatement ps = c.prepareStatement("SELECT pg_get_indexdef(x.indexrelid) FROM pg_index x"
                + " WHERE x.indrelid = ?::regclass AND x.indisvalid"
                + " AND NOT EXISTS (SELECT 1 FROM pg_constraint con WHERE con.conindid = x.indexrelid)"
                + " AND NOT EXISTS (SELECT 1 FROM pg_inherits h WHERE h.inhrelid = x.indexrelid)")) {
            ps.setString(1, relation);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (body.equals(indexBody(rs.getString(1)))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Each partitioned table's partitions, largest first, when it has at least two and all of them are
     * plain tables; a table partitioned again below is built from its parent as before. Puts each
     * partition's heap size in {@code bytes}.
     */
    private Map<String, List<String>> leafPartitions(List<String> tables, Map<String, Long> bytes)
            throws SQLException {
        Map<String, List<String>> out = new HashMap<>();
        Set<String> nested = new java.util.HashSet<>();
        String sql = """
                SELECT p.relname, c.relname, c.relkind, pg_relation_size(c.oid)
                FROM pg_inherits h
                JOIN pg_class c ON c.oid = h.inhrelid
                JOIN pg_class p ON p.oid = h.inhparent
                JOIN pg_namespace n ON n.oid = p.relnamespace
                WHERE n.nspname = ? AND p.relkind = 'p' AND p.relname = ANY (?::text[])
                ORDER BY p.relname, pg_relation_size(c.oid) DESC, c.relname
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, schema);
            ps.setArray(2, conn.createArrayOf("text", tables.toArray()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (!"r".equals(rs.getString(3))) {
                        nested.add(rs.getString(1));
                    }
                    out.computeIfAbsent(rs.getString(1), k -> new ArrayList<>()).add(rs.getString(2));
                    bytes.put(rs.getString(2), rs.getLong(4));
                }
            }
        }
        out.keySet().removeIf(t -> nested.contains(t) || out.get(t).size() < 2);
        return out;
    }

    private void build(Connection c, String snapshotId, Entry e) throws SQLException {
        try (Statement st = c.createStatement();
             PreparedStatement mark = c.prepareStatement("UPDATE " + q() + " SET built = true, built_at = now()"
                     + " WHERE snapshot_id = ? AND kind = ? AND table_name = ? AND name = ?")) {
            st.execute(e.createSql(schema));
            mark.setString(1, snapshotId);
            mark.setString(2, e.kind());
            mark.setString(3, e.table());
            mark.setString(4, e.name());
            mark.executeUpdate();
            c.commit();
        } catch (SQLException ex) {
            c.rollback();
            throw failure(e, ex);
        }
    }

    private static SQLException failure(Entry e, SQLException ex) {
        String hint = e.key() && "23505".equals(ex.getSQLState())
                ? " The loaded rows contain duplicate keys; the snapshot's key derivation or its"
                + " source data does not match this table's key." : "";
        return new SQLException("Rebuilding " + e.label() + " failed: " + ex.getMessage() + hint,
                ex.getSQLState(), ex);
    }

    /**
     * Problems that make the schema unfit to use: an entry not rebuilt, missing, invalid, or with a
     * definition other than the one captured. Empty when this snapshot deferred nothing.
     */
    public List<String> problems(String snapshotId) throws SQLException {
        List<String> out = new ArrayList<>();
        if (!exists()) {
            return out;
        }
        for (Entry e : entries(snapshotId)) {
            if (!e.built()) {
                out.add(e.label() + " has not been rebuilt");
                continue;
            }
            String live = CONSTRAINT.equals(e.kind()) ? liveConstraint(e) : liveIndex(e);
            if (live == null) {
                out.add(e.label() + " is missing or invalid");
            } else if (!live.equals(e.definition())) {
                out.add(e.label() + " differs: expected [" + e.definition() + "] but found [" + live + "]");
            }
        }
        return out;
    }

    private String liveConstraint(Entry e) throws SQLException {
        String sql = """
                SELECT pg_get_constraintdef(con.oid)
                FROM pg_constraint con
                JOIN pg_class t ON t.oid = con.conrelid
                JOIN pg_namespace n ON n.oid = t.relnamespace
                LEFT JOIN pg_index x ON x.indexrelid = con.conindid
                WHERE n.nspname = ? AND t.relname = ? AND con.conname = ?
                  AND (x.indexrelid IS NULL OR x.indisvalid)
                """;
        return single(sql, e.table(), e.name());
    }

    private String liveIndex(Entry e) throws SQLException {
        String sql = """
                SELECT pg_get_indexdef(x.indexrelid)
                FROM pg_index x
                JOIN pg_class i ON i.oid = x.indexrelid
                JOIN pg_class t ON t.oid = x.indrelid
                JOIN pg_namespace n ON n.oid = t.relnamespace
                WHERE n.nspname = ? AND t.relname = ? AND i.relname = ? AND x.indisvalid
                """;
        return single(sql, e.table(), e.name());
    }

    private String single(String sql, String table, String name) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, schema);
            ps.setString(2, table);
            ps.setString(3, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    public List<Entry> entries(String snapshotId) throws SQLException {
        List<Entry> out = new ArrayList<>();
        if (!exists()) {
            return out;
        }
        try (PreparedStatement ps = conn.prepareStatement("SELECT table_name, name, kind, is_key, definition,"
                + " key_columns, built FROM " + q() + " WHERE snapshot_id = ? ORDER BY table_name, kind, name")) {
            ps.setString(1, snapshotId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new Entry(rs.getString(1), rs.getString(2), rs.getString(3), rs.getBoolean(4),
                            rs.getString(5), splitColumns(rs.getString(6)), rs.getBoolean(7)));
                }
            }
        }
        return out;
    }

    /**
     * The primary key a deferred table will get back, so schema checks on a resumed import see the
     * table as it was created rather than as it is mid-load. Empty when none is recorded.
     */
    static List<String> recordedPrimaryKey(Connection conn, String schema, String table) throws SQLException {
        if (!exists(conn, schema)) {
            return List.of();
        }
        try (PreparedStatement ps = conn.prepareStatement("SELECT key_columns FROM "
                + Identifiers.quote(schema) + "." + Identifiers.quote(TABLE)
                + " WHERE table_name = ? AND kind = ? AND key_columns IS NOT NULL AND NOT built LIMIT 1")) {
            ps.setString(1, table);
            ps.setString(2, CONSTRAINT);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? splitColumns(rs.getString(1)) : List.of();
            }
        }
    }

    private static List<String> splitColumns(String columns) {
        return columns == null || columns.isEmpty() ? List.of() : List.of(columns.split(","));
    }

    private List<Entry> capture(List<String> tables) throws SQLException {
        List<Entry> out = new ArrayList<>();
        if (tables.isEmpty()) {
            return out;
        }
        List<String> all = new ArrayList<>(tables);
        all.addAll(partitionsOf(tables));
        String[] names = all.toArray(String[]::new);
        PgSchema pgSchema = new PgSchema(conn, schema);

        // Constraints declared on the table itself; a partition's copy of its parent's key has a parent.
        String constraints = """
                SELECT t.relname, con.conname, pg_get_constraintdef(con.oid), con.contype
                FROM pg_constraint con
                JOIN pg_class t ON t.oid = con.conrelid
                JOIN pg_namespace n ON n.oid = t.relnamespace
                WHERE n.nspname = ? AND t.relname = ANY (?) AND con.contype IN ('p', 'u', 'x')
                  AND con.conparentid = 0
                ORDER BY t.relname, con.conname
                """;
        try (PreparedStatement ps = conn.prepareStatement(constraints)) {
            ps.setString(1, schema);
            ps.setArray(2, conn.createArrayOf("text", names));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String table = rs.getString(1);
                    List<String> keyColumns = "p".equals(rs.getString(4)) ? pgSchema.primaryKey(table) : List.of();
                    out.add(new Entry(table, rs.getString(2), CONSTRAINT, true, rs.getString(3), keyColumns, false));
                }
            }
        }
        // Indexes that back none of those constraints and are not a partition's copy of a parent index.
        String indexes = """
                SELECT t.relname, i.relname, pg_get_indexdef(x.indexrelid), x.indisunique
                FROM pg_index x
                JOIN pg_class i ON i.oid = x.indexrelid
                JOIN pg_class t ON t.oid = x.indrelid
                JOIN pg_namespace n ON n.oid = t.relnamespace
                WHERE n.nspname = ? AND t.relname = ANY (?)
                  AND NOT EXISTS (SELECT 1 FROM pg_constraint con
                                  WHERE con.conindid = x.indexrelid AND con.conrelid = x.indrelid
                                    AND con.contype IN ('p', 'u', 'x'))
                  AND NOT EXISTS (SELECT 1 FROM pg_inherits h WHERE h.inhrelid = x.indexrelid)
                ORDER BY t.relname, i.relname
                """;
        try (PreparedStatement ps = conn.prepareStatement(indexes)) {
            ps.setString(1, schema);
            ps.setArray(2, conn.createArrayOf("text", names));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new Entry(rs.getString(1), rs.getString(2), INDEX, rs.getBoolean(4),
                            rs.getString(3), List.of(), false));
                }
            }
        }
        return out;
    }

    private void record(String snapshotId, List<Entry> entries) throws SQLException {
        boolean autoCommit = conn.getAutoCommit();
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO " + q()
                + " (snapshot_id, table_name, name, kind, is_key, definition, key_columns) VALUES (?,?,?,?,?,?,?)")) {
            conn.setAutoCommit(false);
            for (Entry e : entries) {
                ps.setString(1, snapshotId);
                ps.setString(2, e.table());
                ps.setString(3, e.name());
                ps.setString(4, e.kind());
                ps.setBoolean(5, e.key());
                ps.setString(6, e.definition());
                ps.setString(7, e.keyColumns().isEmpty() ? null : String.join(",", e.keyColumns()));
                ps.addBatch();
            }
            ps.executeBatch();
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(autoCommit);
        }
    }

    private List<String> partitionsOf(List<String> tables) throws SQLException {
        return new ArrayList<>(partitionParents(null, tables).keySet());
    }

    /** Partition name to parent name, optionally limited to the given partitions or parents. */
    private Map<String, String> partitionParents(List<String> children, List<String> parents) throws SQLException {
        Map<String, String> out = new LinkedHashMap<>();
        String sql = """
                SELECT c.relname, p.relname
                FROM pg_inherits h
                JOIN pg_class c ON c.oid = h.inhrelid
                JOIN pg_class p ON p.oid = h.inhparent
                JOIN pg_namespace n ON n.oid = p.relnamespace
                WHERE n.nspname = ? AND c.relkind IN ('r', 'p')
                  AND (?::text[] IS NULL OR c.relname = ANY (?::text[]))
                  AND (?::text[] IS NULL OR p.relname = ANY (?::text[]))
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            java.sql.Array c = children == null ? null : conn.createArrayOf("text", children.toArray());
            java.sql.Array p = parents == null ? null : conn.createArrayOf("text", parents.toArray());
            ps.setString(1, schema);
            ps.setArray(2, c);
            ps.setArray(3, c);
            ps.setArray(4, p);
            ps.setArray(5, p);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.put(rs.getString(1), rs.getString(2));
                }
            }
        }
        return out;
    }

    /** Tables on either side of a foreign key keep their indexes: a referenced key cannot be dropped. */
    private Set<String> referencedByForeignKeys(Collection<String> tables) throws SQLException {
        Set<String> out = new TreeSet<>();
        String sql = """
                SELECT t.relname
                FROM pg_constraint con
                JOIN pg_class t ON t.oid IN (con.conrelid, con.confrelid)
                JOIN pg_namespace n ON n.oid = t.relnamespace
                WHERE con.contype = 'f' AND n.nspname = ? AND t.relname = ANY (?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, schema);
            ps.setArray(2, conn.createArrayOf("text", tables.toArray()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(rs.getString(1));
                }
            }
        }
        return out;
    }

    /**
     * Converts a DuckDB-style memory limit ({@code "3GB"}, {@code "512MiB"}) to a Postgres setting in
     * megabytes, or null when it cannot be read.
     */
    static String postgresMemory(String limit) {
        if (limit == null) {
            return null;
        }
        Matcher m = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*([KMGT]?)I?B?").matcher(limit.trim().toUpperCase(Locale.ROOT));
        if (!m.matches()) {
            return null;
        }
        double n = Double.parseDouble(m.group(1));
        double mb = switch (m.group(2)) {
            case "K" -> n / 1024;
            case "M" -> n;
            case "G" -> n * 1024;
            case "T" -> n * 1024 * 1024;
            default -> n / (1024 * 1024);
        };
        long rounded = (long) mb;
        return rounded >= 1 ? rounded + "MB" : null;
    }
}

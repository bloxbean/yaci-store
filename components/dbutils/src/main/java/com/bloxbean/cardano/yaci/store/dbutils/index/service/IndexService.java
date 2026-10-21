package com.bloxbean.cardano.yaci.store.dbutils.index.service;

import com.bloxbean.cardano.yaci.store.dbutils.index.model.IndexDefinition;
import com.bloxbean.cardano.yaci.store.dbutils.index.model.TableIndex;
import com.bloxbean.cardano.yaci.store.dbutils.index.util.DatabaseUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexService {

    private final JdbcTemplate jdbcTemplate;
    private final DatabaseUtils databaseUtils;

    public Pair<List<IndexDefinition.Index>, List<IndexDefinition.Index>> applyIndexes(List<IndexDefinition> indexDefinitions) {
        var dbTypeOpt = DatabaseUtils.getDbType(jdbcTemplate.getDataSource());
        if (dbTypeOpt.isEmpty()) {
            log.error("Apply Index failure. Couldn't detect database type.");
        }

        log.info("Db type : {}", dbTypeOpt.get());

        List<IndexDefinition.Index> success = new ArrayList<>();
        List<IndexDefinition.Index> failure = new ArrayList<>();

        for (IndexDefinition indexDefinition : indexDefinitions) {
            String tableName = indexDefinition.getTable();
            if (databaseUtils.tableExists(tableName)) {
                for (IndexDefinition.Index index : indexDefinition.getIndexes()) {
                    String sql = generateCreateIndexSQL(tableName, index, dbTypeOpt.get());
                    if (sql != null) {
                        log.info("Applying index on table {}, index name {} ", indexDefinition.getTable(), index.getName());
                        try {
                            log.debug(sql);
                            jdbcTemplate.execute(sql);
                            success.add(index);
                        } catch (Exception e) {
                            log.warn("Failed to apply index on table {}, index name {} ", indexDefinition.getTable(), index.getName());
                            log.warn("Index query: {}", sql);
                            failure.add(index);
                        }
                    } else {
                        log.warn("Failed to apply index on table {}, index name {} ", indexDefinition.getTable(), index.getName());
                        log.warn("Index query is null");
                        failure.add(index);
                    }
                }
            }
        }

        return Pair.of(success, failure);
    }

    /**
     * Applies the indexes on up to {@code parallelism} connections. With one, this is
     * {@link #applyIndexes(List)}. Otherwise the indexes of the largest tables start first, so the
     * longest builds do not begin last; PostgreSQL builds several indexes on one table at the same time
     * (each {@code CREATE INDEX} takes a share lock). Results keep the definitions' order.
     */
    public Pair<List<IndexDefinition.Index>, List<IndexDefinition.Index>> applyIndexes(List<IndexDefinition> indexDefinitions,
                                                                                    int parallelism) {
        if (parallelism <= 1) {
            return applyIndexes(indexDefinitions);
        }
        var dbTypeOpt = DatabaseUtils.getDbType(jdbcTemplate.getDataSource());
        if (dbTypeOpt.isEmpty()) {
            log.error("Apply Index failure. Couldn't detect database type.");
            return Pair.of(List.of(), List.of());
        }
        log.info("Db type : {}, applying indexes on {} connections", dbTypeOpt.get(), parallelism);

        record Task(int order, String table, IndexDefinition.Index index, String sql) {}
        List<Task> tasks = new ArrayList<>();
        Map<String, Long> sizes = new HashMap<>();
        for (IndexDefinition indexDefinition : indexDefinitions) {
            String tableName = indexDefinition.getTable();
            if (!databaseUtils.tableExists(tableName)) {
                continue;
            }
            sizes.computeIfAbsent(tableName, t -> tableSize(t, dbTypeOpt.get()));
            for (IndexDefinition.Index index : indexDefinition.getIndexes()) {
                tasks.add(new Task(tasks.size(), tableName, index, generateCreateIndexSQL(tableName, index, dbTypeOpt.get())));
            }
        }
        List<Task> ordered = new ArrayList<>(tasks);
        ordered.sort(Comparator.comparingLong((Task t) -> sizes.getOrDefault(t.table(), 0L)).reversed()
                .thenComparingInt(Task::order));

        boolean[] applied = new boolean[tasks.size()];
        AtomicInteger done = new AtomicInteger();
        long started = System.currentTimeMillis();
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(parallelism, Math.max(1, ordered.size())));
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (Task task : ordered) {
                futures.add(pool.submit(() -> {
                    if (task.sql() == null) {
                        log.warn("Failed to apply index on table {}, index name {} ", task.table(), task.index().getName());
                        log.warn("Index query is null");
                        return;
                    }
                    long t0 = System.currentTimeMillis();
                    log.info("Applying index on table {}, index name {} ", task.table(), task.index().getName());
                    try {
                        log.debug(task.sql());
                        jdbcTemplate.execute(task.sql());
                        applied[task.order()] = true;
                        log.info("Applied index {} on {} in {}s ({}/{} done)", task.index().getName(), task.table(),
                                (System.currentTimeMillis() - t0) / 1000, done.incrementAndGet(), ordered.size());
                    } catch (Exception e) {
                        done.incrementAndGet();
                        log.warn("Failed to apply index on table {}, index name {} ", task.table(), task.index().getName());
                        log.warn("Index query: {}", task.sql());
                    }
                }));
            }
            for (Future<?> f : futures) {
                f.get();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while applying indexes", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Applying indexes failed", e.getCause());
        } finally {
            pool.shutdownNow();
        }

        List<IndexDefinition.Index> success = new ArrayList<>();
        List<IndexDefinition.Index> failure = new ArrayList<>();
        for (Task task : tasks) {
            (applied[task.order()] ? success : failure).add(task.index());
        }
        log.info("Applied {} index(es), {} failed, in {}s", success.size(), failure.size(),
                (System.currentTimeMillis() - started) / 1000);
        return Pair.of(success, failure);
    }

    /** Bytes on disk of a table, partitions included; 0 when unknown. */
    private long tableSize(String tableName, DatabaseUtils.DbType dbType) {
        if (dbType != DatabaseUtils.DbType.postgres) {
            return 0;
        }
        try {
            Long size = jdbcTemplate.queryForObject("SELECT COALESCE(sum(pg_total_relation_size(c.oid)), 0)"
                    + " FROM pg_class c WHERE c.oid = to_regclass(?)"
                    + " OR c.oid IN (SELECT inhrelid FROM pg_inherits WHERE inhparent = to_regclass(?))",
                    Long.class, tableName, tableName);
            return size == null ? 0 : size;
        } catch (Exception e) {
            return 0;
        }
    }

    private String generateCreateIndexSQL(String tableName, IndexDefinition.Index index, DatabaseUtils.DbType dbType) {
        String indexName = index.getName();
        String columns = String.join(", ", index.getColumns());

        switch (dbType) {
            case postgres:
                if (index.getExcludes() != null && index.getExcludes().contains(DatabaseUtils.DbType.postgres.toString()))
                    return null;

                String using = index.getType() != null? "USING " + index.getType().trim() : "";
                String where = index.getWhere() != null && !index.getWhere().trim().isEmpty()
                        ? " WHERE " + index.getWhere().trim()
                        : "";
                return String.format("CREATE INDEX IF NOT EXISTS %s ON %s %s (%s) %s;", indexName, tableName, using, columns, where);
            case h2:
                if (index.getExcludes() != null && index.getExcludes().contains(DatabaseUtils.DbType.h2.toString()))
                    return null;

                return String.format("CREATE INDEX IF NOT EXISTS %s ON %s (%s);", indexName, tableName, columns);
            case mysql:
                if (index.getExcludes() != null && index.getExcludes().contains(DatabaseUtils.DbType.mysql.toString()))
                    return null;

                return String.format("CREATE INDEX %s ON %s (%s);", indexName, tableName, columns);
            default:
                log.error("Unsupported DB Type. It shouldn't reach here. DB Type: " + dbType);
                return null;
        }
    }

    public Pair<List<TableIndex>, List<TableIndex>> verifyIndexes(List<IndexDefinition> indexDefinitions) {
        var dbTypeOpt = DatabaseUtils.getDbType(jdbcTemplate.getDataSource());
        if (dbTypeOpt.isEmpty()) {
            log.error("Apply Index failure. Couldn't detect database type.");
        }

        log.debug("Db type : {}", dbTypeOpt.get());

        List<TableIndex> exists = new ArrayList<>();
        List<TableIndex> notExists = new ArrayList<>();

        for (IndexDefinition indexDefinition : indexDefinitions) {
            String tableName = indexDefinition.getTable();
            for (IndexDefinition.Index index : indexDefinition.getIndexes()) {
                if (databaseUtils.indexExists(tableName, index.getName()))
                    exists.add(new TableIndex(tableName, index.getName()));
                else
                    notExists.add(new TableIndex(tableName, index.getName()));
            }
        }

        return Pair.of(exists, notExists);
    }
}

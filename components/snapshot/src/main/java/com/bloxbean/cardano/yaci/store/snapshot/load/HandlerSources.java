package com.bloxbean.cardano.yaci.store.snapshot.load;

import com.bloxbean.cardano.yaci.store.snapshot.ducklake.DuckDb;
import com.bloxbean.cardano.yaci.store.snapshot.handler.HandlerRegistry;
import com.bloxbean.cardano.yaci.store.snapshot.manifest.SnapshotManifest;
import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotSpecRegistry;
import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotTableSpec;
import com.bloxbean.cardano.yaci.store.snapshot.util.Identifiers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Rows a control-state handler reads from a table this import does not load.
 *
 * <p>The cursor and era handlers read the block table. A target that does not run the blocks store
 * (a metadata or script indexer, say) imports no block rows, yet still needs its cursor tail and era
 * transitions. The block files are therefore extracted anyway, and only the rows those handlers
 * read -- the newest {@code cursor-no-of-blocks-to-keep} blocks up to the point and the first block
 * of each era -- are copied into a scratch table for them, then dropped. The block table stays
 * empty, as it would on a node that never ran the store.
 */
final class HandlerSources {

    /** Scratch tables share the import's prefix, which schema checks and the fingerprint skip. */
    static final String PREFIX = "_yaci_snapshot_import_src_";

    private static final String BLOCK = "block";

    private HandlerSources() {
    }

    /** Specification ids the selected handlers read but this import does not load. */
    static Set<String> unloaded(SnapshotSpecRegistry registry, HandlerRegistry handlers) {
        Set<String> imported = new TreeSet<>();
        registry.importedTables().forEach(s -> imported.add(s.id()));
        Set<String> out = new TreeSet<>();
        for (SnapshotTableSpec spec : registry.handlerTables()) {
            for (String read : handlers.get(spec.importSpec().handler()).reads()) {
                if (!imported.contains(read) && registry.byId(read).isPresent()) {
                    out.add(read);
                }
            }
        }
        return out;
    }

    /** Why the unloaded tables cannot be staged, or nothing. */
    static List<String> problems(SnapshotSpecRegistry registry, HandlerRegistry handlers) {
        List<String> out = new ArrayList<>();
        for (String id : unloaded(registry, handlers)) {
            if (!BLOCK.equals(id)) {
                out.add("A control-state handler reads '" + id + "', which this import does not load and"
                        + " cannot stage; enable its store");
            }
        }
        return out;
    }

    /**
     * Copies the rows the handlers read from each unloaded table's files.
     *
     * @return target table to the qualified scratch relation, for {@code SnapshotHandler.Context}
     */
    static Map<String, String> stage(SnapshotSpecRegistry registry, HandlerRegistry handlers,
                                     SnapshotManifest manifest, ImportOptions options, Connection pg,
                                     TableLoader loader) throws SQLException {
        Map<String, String> staged = new LinkedHashMap<>();
        for (String id : unloaded(registry, handlers)) {
            if (!BLOCK.equals(id)) {
                throw new IllegalStateException(problems(registry, handlers).get(0));
            }
            SnapshotTableSpec spec = registry.byId(id).orElseThrow();
            SnapshotManifest.TableManifest table = manifest.table(id);
            if (table == null || table.files() == null || table.files().isEmpty()) {
                throw new IllegalStateException("The snapshot has no " + spec.targetTable()
                        + " files to rebuild the cursor and era from");
            }
            String scratch = PREFIX + spec.targetTable();
            String qualified = Identifiers.qualified(
                    Identifiers.requireSqlIdentifier(options.schema(), "target schema"), scratch);
            try (Statement st = pg.createStatement()) {
                st.execute(dropSql(qualified));
                st.execute(createSql(qualified));
            }
            Path spill = options.workDir().resolve("spill-sources");
            try {
                Files.createDirectories(spill);
            } catch (IOException e) {
                throw new SQLException("Unable to create spill directory " + spill, e);
            }
            long point = manifest.point().blockNumber();
            String src = "(SELECT hash, slot, number, era, prev_hash FROM "
                    + DuckDb.readParquet(loader.parquetList(table.files()))
                    + " WHERE number >= 0 AND number <= " + point + ")";
            try (DuckPgSession session = DuckPgSession.open(options, spill, 2)) {
                session.exec("INSERT INTO " + session.qualify(scratch)
                        + " SELECT * FROM (SELECT * FROM " + src + " ORDER BY number DESC LIMIT "
                        + Math.max(1, options.cursorBlocksToKeep()) + ")"
                        + " UNION"
                        + " SELECT hash, slot, number, era, prev_hash FROM (SELECT *, row_number() OVER"
                        + " (PARTITION BY era ORDER BY number) AS rn FROM " + src + ") WHERE rn = 1");
            }
            staged.put(spec.targetTable(), qualified);
        }
        return staged;
    }

    static void drop(Connection pg, Map<String, String> staged) throws SQLException {
        try (Statement st = pg.createStatement()) {
            for (String relation : staged.values()) {
                st.execute(dropSql(relation));
            }
        }
    }

    /** {@code relation} is a scratch table {@link #stage} qualified with {@link Identifiers#qualified}. */
    private static String dropSql(String relation) {
        return "DROP TABLE IF EXISTS " + relation;
    }

    private static String createSql(String relation) {
        return "CREATE UNLOGGED TABLE " + relation
                + " (hash text, slot bigint, number bigint, era integer, prev_hash text)";
    }
}

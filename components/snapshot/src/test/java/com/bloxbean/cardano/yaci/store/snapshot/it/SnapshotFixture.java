package com.bloxbean.cardano.yaci.store.snapshot.it;

import com.bloxbean.cardano.yaci.store.snapshot.archive.ArchiveWriter;
import com.bloxbean.cardano.yaci.store.snapshot.manifest.ConsistencyPoint;
import com.bloxbean.cardano.yaci.store.snapshot.manifest.ManifestCodec;
import com.bloxbean.cardano.yaci.store.snapshot.manifest.SnapshotManifest;
import com.bloxbean.cardano.yaci.store.snapshot.spec.RestoreMode;
import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotSpecRegistry;
import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotTableSpec;
import com.bloxbean.cardano.yaci.store.snapshot.util.Digests;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builds a small but structurally faithful snapshot: real Parquet written by DuckDB in the exact
 * shape the analytics exporters produce, packaged into real ZIP parts with a real manifest.
 *
 * <p>Faithful shape matters more than size here. The fixture exercises a timestamp column, three
 * jsonb columns, a rename, a smallint narrowing, the flattened {@code address_utxo} regrouping and
 * the block join, which is where an import goes quietly wrong.
 */
final class SnapshotFixture {

    static final int BLOCKS = 40;
    static final int POINT_EPOCH = 2;
    /** Blocks 0..39; epoch 2 covers blocks 20..29, so the point is block 29. */
    static final int POINT_BLOCK = 29;

    private SnapshotFixture() {
    }

    record Built(Path archiveDir, Path manifestPath, SnapshotManifest manifest) {}

    static final List<String> ALL_TABLES = List.of("block", "epoch", "address-utxo", "adapot", "rollback");

    static Built build(Path root, String schemaFingerprint, String flywayFingerprint) throws Exception {
        return build(root, schemaFingerprint, flywayFingerprint, ALL_TABLES, "it-fixture-snapshot");
    }

    static Built build(Path root, String schemaFingerprint, String flywayFingerprint,
                       List<String> includedSpecIds, String snapshotId) throws Exception {
        Path dataDir = root.resolve("data");
        Path archiveDir = root.resolve("archive");
        Files.createDirectories(dataDir);

        try (Connection duck = DriverManager.getConnection("jdbc:duckdb:");
             Statement st = duck.createStatement()) {
            writeBlocks(st, dataDir);
            writeEpochs(st, dataDir);
            writeAddressUtxo(st, dataDir);
            writeAdaPot(st, dataDir);
            writeRollback(st, dataDir);
        }

        SnapshotSpecRegistry registry = SnapshotSpecRegistry.builtIn();
        List<SnapshotManifest.FileEntry> allFiles = new ArrayList<>();
        List<String> owners = new ArrayList<>();
        List<SnapshotManifest.TableManifest> tables = new ArrayList<>();

        for (String specId : includedSpecIds) {
            SnapshotTableSpec spec = registry.byId(specId).orElseThrow();
            List<SnapshotManifest.FileEntry> files = collect(dataDir, "main/" + spec.relation());
            for (SnapshotManifest.FileEntry f : files) {
                allFiles.add(f);
                owners.add(specId);
            }
            long rows = expectedRows(dataDir, spec, files);
            tables.add(new SnapshotManifest.TableManifest(spec.id(), spec.specVersion(), spec.digest(),
                    spec.module(), spec.kind().name(), spec.restore().name(), spec.reason(),
                    spec.relation(), spec.source().exporterId(), spec.targetTable(),
                    spec.importSpec().mode().name(), spec.consistency().cutoff().type().name(),
                    spec.importSpec().transformVersion(), spec.importSpec().dependencies(),
                    rows, spec.validation().key(), "fixture", declaredColumns(spec.relation()),
                    Map.of(), files));
        }
        // Every other classified table is declared with zero files so preflight sees full coverage.
        for (SnapshotTableSpec spec : registry.all()) {
            if (tables.stream().anyMatch(t -> t.specId().equals(spec.id()))) {
                continue;
            }
            tables.add(new SnapshotManifest.TableManifest(spec.id(), spec.specVersion(), spec.digest(),
                    spec.module(), spec.kind().name(), spec.restore().name(), spec.reason(),
                    spec.relation(), spec.hasSource() ? spec.source().exporterId() : null,
                    spec.targetTable(),
                    spec.restore() == RestoreMode.IMPORT ? spec.importSpec().mode().name() : null,
                    null, 1, List.of(), 0, List.of(), null, Map.of(), Map.of(), List.of()));
        }

        // Small parts so the fixture exercises multi-part packaging and resume across parts.
        List<SnapshotManifest.PartManifest> parts =
                new ArchiveWriter(dataDir, archiveDir, "it-snap", 32 * 1024).write(allFiles, owners, null);

        SnapshotManifest manifest = new SnapshotManifest(ManifestCodec.FORMAT_VERSION,
                snapshotId,
                "2026-01-01T00:00:00Z", "integration-test", "test", "1", "duckdb", "1.0",
                point(), null, List.of("blocks", "utxo", "epoch-aggr", "adapot"),
                Map.of("store.utxo.pruning-enabled", "false"),
                schemaFingerprint, flywayFingerprint, tables, parts, List.of());

        Path manifestPath = archiveDir.resolve("it-snap.manifest.json");
        new ManifestCodec().writeAtomically(manifest, manifestPath);
        return new Built(archiveDir, manifestPath, manifest);
    }

    /**
     * The DuckLake column types the producing catalog records, copied from the real preprod export
     * so the fixture manifest carries the same information a real one does. DuckDB widens INT32 to
     * BIGINT when reading Parquet, which is exactly why the importer plans from these rather than
     * from what it sees in the files.
     */
    private static Map<String, String> declaredColumns(String relation) {
        return switch (relation) {
            case "block" -> ordered("hash", "varchar", "number", "int64", "body_hash", "varchar",
                    "body_size", "int32", "epoch", "int32", "total_output", "decimal(38,0)",
                    "total_fees", "int64", "block_time", "timestamptz", "era", "int16",
                    "issuer_vkey", "varchar", "leader_vrf", "varchar", "nonce_vrf", "varchar",
                    "prev_hash", "varchar", "protocol_version", "varchar", "slot", "int64",
                    "vrf_result", "varchar", "vrf_vkey", "varchar", "no_of_txs", "int32",
                    "slot_leader", "varchar", "epoch_slot", "int32", "op_cert_hot_vkey", "varchar",
                    "op_cert_seq_number", "int64", "op_cert_kes_period", "int64",
                    "op_cert_sigma", "varchar", "date", "date");
            case "epoch" -> ordered("epoch", "int64", "block_count", "int32",
                    "transaction_count", "int64", "total_output", "decimal(38,0)",
                    "total_fees", "int64", "start_time", "int64", "end_time", "int64",
                    "max_slot", "int64");
            case "address_utxo" -> ordered("tx_hash", "varchar", "output_index", "int16",
                    "asset_unit", "varchar", "policy_id", "varchar", "asset_name", "varchar",
                    "quantity", "decimal(38,0)", "owner_addr", "varchar", "owner_stake_addr", "varchar",
                    "owner_payment_credential", "varchar", "owner_stake_credential", "varchar",
                    "inline_datum", "varchar", "data_hash", "varchar", "script_ref", "varchar",
                    "reference_script_hash", "varchar", "is_collateral_return", "boolean",
                    "epoch", "int32", "slot", "int64", "block_hash", "varchar",
                    "block_time", "timestamptz", "date", "date");
            case "adapot" -> ordered("epoch", "int32", "slot", "int64",
                    "deposits_stake", "decimal(38,0)", "fees", "decimal(38,0)", "utxo", "decimal(38,0)",
                    "treasury", "decimal(38,0)", "reserves", "decimal(38,0)", "circulation", "decimal(38,0)",
                    "distributed_rewards", "decimal(38,0)", "undistributed_rewards", "decimal(38,0)",
                    "rewards_pot", "decimal(38,0)", "pool_rewards_pot", "decimal(38,0)");
            case "rollback" -> ordered("id", "int64", "rollback_to_block_hash", "varchar",
                    "rollback_to_slot", "int64", "current_block_hash", "varchar",
                    "current_slot", "int64", "current_block", "int64",
                    "block_time", "timestamptz", "date", "date");
            default -> Map.of();
        };
    }

    private static Map<String, String> ordered(String... pairs) {
        java.util.LinkedHashMap<String, String> m = new java.util.LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            m.put(pairs[i], pairs[i + 1]);
        }
        return m;
    }

    /**
     * Rows the import will produce for this table: the source rows at or below the cutoff, or the
     * number of distinct source keys when the transform regroups. Mirrors {@code ExportPlanner}.
     */
    private static long expectedRows(Path dataDir, SnapshotTableSpec spec,
                                     List<SnapshotManifest.FileEntry> files) throws SQLException {
        if (files.isEmpty()) {
            return 0;
        }
        StringBuilder list = new StringBuilder("[");
        for (int i = 0; i < files.size(); i++) {
            if (i > 0) {
                list.append(", ");
            }
            list.append('\'').append(dataDir.resolve(files.get(i).path()).toAbsolutePath()).append('\'');
        }
        list.append(']');

        SnapshotTableSpec.CutoffRule cutoff = spec.consistency().cutoff();
        long cut = switch (cutoff.type()) {
            case NONE -> Long.MAX_VALUE;
            case SLOT_LTE -> slotOf(POINT_BLOCK);
            case EPOCH_LTE -> POINT_EPOCH;
            case EPOCH_LTE_OFFSET -> POINT_EPOCH - cutoff.offset();
        };
        String where = cutoff.type() == com.bloxbean.cardano.yaci.store.snapshot.spec.CutoffType.NONE
                ? "" : " WHERE \"" + cutoff.column() + "\" <= " + cut;
        List<String> sourceKey = spec.validation().sourceKey();
        String select = sourceKey.isEmpty()
                ? "SELECT count(*) FROM read_parquet(" + list + ")" + where
                : "SELECT count(*) FROM (SELECT DISTINCT "
                + String.join(", ", sourceKey.stream().map(c -> "\"" + c + "\"").toList())
                + " FROM read_parquet(" + list + ")" + where + ")";

        try (Connection duck = DriverManager.getConnection("jdbc:duckdb:");
             Statement st = duck.createStatement();
             var rs = st.executeQuery(select)) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }

    static ConsistencyPoint point() {
        return new ConsistencyPoint("preprod", 1, POINT_EPOCH, slotOf(POINT_BLOCK), POINT_BLOCK,
                hashOf(POINT_BLOCK), hashOf(POINT_BLOCK - 1), eraOf(POINT_BLOCK),
                1700000000L + slotOf(POINT_BLOCK), 1);
    }

    static String hashOf(long block) {
        return String.format("%064x", block + 1);
    }

    static long slotOf(long block) {
        return block * 20;
    }

    /** Blocks 0-4 are Byron, so the era handler's Byron exclusion is exercised. */
    static int eraOf(long block) {
        return block < 5 ? 1 : block < 10 ? 5 : block < 20 ? 6 : 7;
    }

    /** DuckDB's COPY TO does not create parent directories. */
    private static String parquetTarget(Path dir) throws IOException {
        Files.createDirectories(dir);
        return dir.resolve("data.parquet").toAbsolutePath().toString();
    }

    private static void writeBlocks(Statement st, Path dataDir) throws SQLException, IOException {
        // Two date partitions so batching and file-set boundaries are exercised.
        for (int part = 0; part < 2; part++) {
            long from = part * (BLOCKS / 2L);
            long to = from + BLOCKS / 2L;
            String target = parquetTarget(dataDir.resolve("main/block/date=2026-01-0" + (part + 1)));
            st.execute("COPY (SELECT"
                    + " printf('%064x', n + 1) AS hash,"
                    + " n AS number,"
                    + " printf('%064x', n + 1000) AS body_hash,"
                    + " CAST(100 + n AS INTEGER) AS body_size,"
                    + " CAST(n / 10 AS INTEGER) AS epoch,"
                    + " CAST(n * 1000 AS DECIMAL(38,0)) AS total_output,"
                    + " CAST(n AS BIGINT) AS total_fees,"
                    + " to_timestamp(1700000000 + n * 20) AS block_time,"
                    + " CAST(CASE WHEN n < 5 THEN 1 WHEN n < 10 THEN 5 WHEN n < 20 THEN 6 ELSE 7 END"
                    + "      AS SMALLINT) AS era,"
                    + " printf('%064x', n + 2000) AS issuer_vkey,"
                    + " '{\"output\": \"a\"}' AS leader_vrf,"
                    + " '{\"output\": \"b\"}' AS nonce_vrf,"
                    + " CASE WHEN n = 0 THEN NULL ELSE printf('%064x', n) END AS prev_hash,"
                    + " '9.0' AS protocol_version,"
                    + " CAST(n * 20 AS BIGINT) AS slot,"
                    + " '{\"output\": \"c\"}' AS vrf_result,"
                    + " printf('%064x', n + 3000) AS vrf_vkey,"
                    + " CAST(1 AS INTEGER) AS no_of_txs,"
                    + " printf('%056x', n) AS slot_leader,"
                    + " CAST(n % 10 AS INTEGER) AS epoch_slot,"
                    + " NULL::VARCHAR AS op_cert_hot_vkey,"
                    + " NULL::BIGINT AS op_cert_seq_number,"
                    + " NULL::BIGINT AS op_cert_kes_period,"
                    + " NULL::VARCHAR AS op_cert_sigma,"
                    + " CAST(to_timestamp(1700000000 + n * 20) AS DATE) AS date"
                    + " FROM range(" + from + ", " + to + ") t(n))"
                    + " TO '" + target + "' (FORMAT PARQUET)");
        }
    }

    private static void writeEpochs(Statement st, Path dataDir) throws SQLException, IOException {
        for (int epoch = 0; epoch <= 3; epoch++) {
            String target = parquetTarget(dataDir.resolve("main/epoch/epoch=" + epoch));
            st.execute("COPY (SELECT"
                    + " CAST(" + epoch + " AS BIGINT) AS epoch,"
                    + " CAST(10 AS INTEGER) AS block_count,"
                    + " CAST(10 AS BIGINT) AS transaction_count,"
                    + " CAST(1000 AS DECIMAL(38,0)) AS total_output,"
                    + " CAST(10 AS BIGINT) AS total_fees,"
                    + " CAST(" + (1700000000L + epoch * 200L) + " AS BIGINT) AS start_time,"
                    + " CAST(" + (1700000000L + (epoch + 1) * 200L) + " AS BIGINT) AS end_time,"
                    + " CAST(" + ((epoch + 1) * 10 - 1) * 20 + " AS BIGINT) AS max_slot)"
                    + " TO '" + target + "' (FORMAT PARQUET)");
        }
    }

    /** Flattened one-row-per-asset shape, exactly as the analytics view produces it. */
    private static void writeAddressUtxo(Statement st, Path dataDir) throws SQLException, IOException {
        for (int part = 0; part < 2; part++) {
            long from = part * (BLOCKS / 2L);
            long to = from + BLOCKS / 2L;
            String target = parquetTarget(dataDir.resolve("main/address_utxo/date=2026-01-0" + (part + 1)));
            st.execute("COPY (SELECT"
                    + " printf('%064x', n + 50000) AS tx_hash,"
                    + " CAST(0 AS SMALLINT) AS output_index,"
                    + " CASE asset WHEN 'lovelace' THEN 'lovelace'"
                    + "             WHEN 'tok'      THEN printf('%056x', 7) || '746f6b'"
                    + "             WHEN 'longname' THEN printf('%056x', 7) || '6c6f6e676e616d65'"
                    + "             ELSE                 printf('%056x', 7) END AS asset_unit,"
                    + " CASE WHEN asset = 'lovelace' THEN NULL ELSE printf('%056x', 7) END AS policy_id,"
                    + " CASE asset WHEN 'lovelace' THEN 'lovelace'"
                    + "            WHEN 'tok'      THEN 'tok'"
                    + "            WHEN 'longname' THEN 'longname'"
                    + "            ELSE NULL END AS asset_name,"
                    + " CAST(CASE WHEN asset = 'lovelace' THEN 1000000 + n ELSE 5 END AS DECIMAL(38,0)) AS quantity,"
                    + " 'addr_test1' || printf('%040x', n) AS owner_addr,"
                    + " 'stake_test1' || printf('%040x', n) AS owner_stake_addr,"
                    + " printf('%056x', n + 10) AS owner_payment_credential,"
                    + " printf('%056x', n + 20) AS owner_stake_credential,"
                    + " NULL::VARCHAR AS inline_datum,"
                    + " NULL::VARCHAR AS data_hash,"
                    + " NULL::VARCHAR AS script_ref,"
                    + " NULL::VARCHAR AS reference_script_hash,"
                    + " false AS is_collateral_return,"
                    + " CAST(n / 10 AS INTEGER) AS epoch,"
                    + " CAST(n * 20 AS BIGINT) AS slot,"
                    + " printf('%064x', n + 1) AS block_hash,"
                    + " to_timestamp(1700000000 + n * 20) AS block_time,"
                    + " CAST(to_timestamp(1700000000 + n * 20) AS DATE) AS date"
                    // 'tok' is a named asset; the two 'pol...' units share a policy and differ in
                    // asset-name length, and one has an empty name, which is what the analytics view
                    // turns into NULL.
                    + " FROM range(" + from + ", " + to + ") t(n),"
                    + "      (SELECT unnest(['lovelace', 'tok', 'longname', 'empty']) AS asset))"
                    + " TO '" + target + "' (FORMAT PARQUET)");
        }
    }

    private static void writeAdaPot(Statement st, Path dataDir) throws SQLException, IOException {
        for (int epoch = 0; epoch <= 3; epoch++) {
            String target = parquetTarget(dataDir.resolve("main/adapot/epoch=" + epoch));
            st.execute("COPY (SELECT"
                    + " CAST(" + epoch + " AS INTEGER) AS epoch,"
                    + " CAST(" + (epoch * 10L * 20) + " AS BIGINT) AS slot,"
                    + " CAST(1 AS DECIMAL(38,0)) AS deposits_stake,"
                    + " CAST(2 AS DECIMAL(38,0)) AS fees,"
                    + " CAST(3 AS DECIMAL(38,0)) AS utxo,"
                    + " CAST(4 AS DECIMAL(38,0)) AS treasury,"
                    + " CAST(5 AS DECIMAL(38,0)) AS reserves,"
                    + " CAST(6 AS DECIMAL(38,0)) AS circulation,"
                    + " CAST(7 AS DECIMAL(38,0)) AS distributed_rewards,"
                    + " CAST(8 AS DECIMAL(38,0)) AS undistributed_rewards,"
                    + " CAST(9 AS DECIMAL(38,0)) AS rewards_pot,"
                    + " CAST(10 AS DECIMAL(38,0)) AS pool_rewards_pot)"
                    + " TO '" + target + "' (FORMAT PARQUET)");
        }
    }

    private static void writeRollback(Statement st, Path dataDir) throws SQLException, IOException {
        String target = parquetTarget(dataDir.resolve("main/rollback/date=2026-01-01"));
        st.execute("COPY (SELECT"
                + " CAST(n AS BIGINT) AS id,"
                + " printf('%064x', n) AS rollback_to_block_hash,"
                + " CAST(n * 20 AS BIGINT) AS rollback_to_slot,"
                + " printf('%064x', n + 1) AS current_block_hash,"
                + " CAST(n * 20 + 20 AS BIGINT) AS current_slot,"
                + " CAST(n AS BIGINT) AS current_block,"
                + " to_timestamp(1700000000 + n * 20) AS block_time,"
                + " CAST(to_timestamp(1700000000 + n * 20) AS DATE) AS date"
                + " FROM range(1, 6) t(n))"
                + " TO '" + target + "' (FORMAT PARQUET)");
    }

    private static List<SnapshotManifest.FileEntry> collect(Path dataDir, String relativeDir)
            throws IOException, SQLException {
        List<SnapshotManifest.FileEntry> out = new ArrayList<>();
        Path dir = dataDir.resolve(relativeDir);
        if (!Files.isDirectory(dir)) {
            return out;
        }
        List<Path> files;
        try (var walk = Files.walk(dir)) {
            files = walk.filter(p -> p.toString().endsWith(".parquet")).sorted().toList();
        }
        try (Connection duck = DriverManager.getConnection("jdbc:duckdb:");
             Statement st = duck.createStatement()) {
            for (Path f : files) {
                long rows;
                try (var rs = st.executeQuery("SELECT count(*) FROM read_parquet('"
                        + f.toAbsolutePath() + "')")) {
                    rs.next();
                    rows = rs.getLong(1);
                }
                String rel = dataDir.relativize(f).toString();
                out.add(new SnapshotManifest.FileEntry(rel, Files.size(f), Digests.sha256Hex(f), rows,
                        rel.substring(0, rel.lastIndexOf('/'))));
            }
        }
        return out;
    }
}

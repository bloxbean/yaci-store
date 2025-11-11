package com.bloxbean.cardano.yaci.store.test.e2e;

import java.math.BigDecimal;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * The DrepDataComparator class is responsible for comparing distributed representative (drep)
 * distribution data between two database systems: DB Sync and Yaci Store database.
 * It validates the consistency of the drep distribution data and ensures that the amounts
 * for specific epochs match between the two systems.
 */
public class DrepDataComparator {
    static int startEpoch = 740;
    static int endEpoch = 902;

    private static final Path LOG_DIR = Paths.get("logs");
    private static final DateTimeFormatter LOG_TS_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final String RUN_TS = LocalDateTime.now().format(LOG_TS_FORMAT);
    private static final Path LOG_FILE = LOG_DIR.resolve("drep_dist_compare-" + RUN_TS + ".log");

    static {
        try {
            if (!Files.exists(LOG_DIR)) Files.createDirectories(LOG_DIR);
            if (!Files.exists(LOG_FILE)) Files.createFile(LOG_FILE);
            logLine("===== Start DrepDist comparison run =====");
            logLine("Log file: " + LOG_FILE.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Failed to initialize log file: " + e.getMessage());
        }
    }

    // Connection details for the two databases (update these with your real values)
    static String dbSyncUrl = "jdbc:postgresql://<db_sync_host>:<db_sync_port>/cexplorer?currentSchema=public";
    static String dbSyncUser = "<dbsync_user>";
    static String dbSyncPassword = "<dbsync_password>";

    static String storeUrl = "jdbc:postgresql://<store_host>:<store_port>/<db_name>?currentSchema=<schema_name>";
    static String storeDBUser = "<store_user>";
    static String storeDBPassword = "<store_password>";

    // Utility function to convert a byte array to a hex string.
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // Normalize the drep hash: remove any unwanted prefix and convert to lowercase.
    // For DB Sync, the bytea value will be converted to hex (which gives no prefix).
    private static String normalizeHash(String hash) {
        // In case the hash still contains a 0x prefix, remove it.
        if (hash.startsWith("0x") || hash.startsWith("0X")) {
            hash = hash.substring(2);
        }
        return hash.toLowerCase();
    }

    public static int compareDrepDistData(int epoch) {
        String dbSyncQuery = "SELECT dh.raw, d.amount, dh.view FROM drep_distr d " +
                "INNER JOIN drep_hash dh ON dh.id = d.hash_id " +
                "WHERE d.epoch_no = ?";

        String indexerQuery = "SELECT drep_hash, drep_id, amount, drep_type FROM drep_dist WHERE epoch = ?";

        // Use Maps to store the result, keyed by the normalized drep hash.
        Map<String, BigDecimal> dbSyncResults = new HashMap<>();
        Map<String, BigDecimal> indexerResults = new HashMap<>();

        Map<String, String> drepHashToIdMap = new HashMap<>();

        BigDecimal dbSyncAbstainAmount = BigDecimal.ZERO;
        BigDecimal indexerAbstainAmount = BigDecimal.ZERO;

        BigDecimal dbSyncNoConfidenceAmount = BigDecimal.ZERO;
        BigDecimal indexerNoConfidenceAmount = BigDecimal.ZERO;

        // Query DB Sync
        try (Connection conn = DriverManager.getConnection(dbSyncUrl, dbSyncUser, dbSyncPassword);
             PreparedStatement ps = conn.prepareStatement(dbSyncQuery)) {

            ps.setInt(1, epoch);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                // Retrieve the bytea column as a byte array.
                byte[] hashBytes = rs.getBytes("raw");
                String hexHash = hashBytes != null? bytesToHex(hashBytes): null;  // converts to hex string without prefix
                String normalizedHash = hexHash != null? normalizeHash(hexHash) : null;
                BigDecimal amount = rs.getBigDecimal("amount");
                String view = rs.getString("view");

                if (view.contains("abstain")) {
                    dbSyncAbstainAmount = dbSyncAbstainAmount.add(amount);
                } else if (view.contains("no_confidence")) {
                    dbSyncNoConfidenceAmount = dbSyncNoConfidenceAmount.add(amount);
                } else {
                    dbSyncResults.put(normalizedHash, amount);
                }
            }
        } catch (SQLException e) {
            logErrorToFile("DB Sync query error for epoch " + epoch, e);
        }

        // Query Indexer (yaci-store)
        try (Connection conn = DriverManager.getConnection(storeUrl, storeDBUser, storeDBPassword);
             PreparedStatement ps = conn.prepareStatement(indexerQuery)) {

            ps.setInt(1, epoch);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                // Here, drep_hash is already a varchar representing hex (without the \x prefix)
                String hash = rs.getString("drep_hash");
                String normalizedHash = normalizeHash(hash);
                BigDecimal amount = rs.getBigDecimal("amount");
                String drepId = rs.getString("drep_id");

                String drepType = rs.getString("drep_type");
                if (drepType.equals("ABSTAIN")) {
                    indexerAbstainAmount = amount;
                } else if (drepType.equals("NO_CONFIDENCE")) {
                    indexerNoConfidenceAmount = amount;
                } else {
                    indexerResults.put(normalizedHash, amount);
                }

                if (drepId != null)
                    drepHashToIdMap.put(hash, drepId);

            }
        } catch (SQLException e) {
            logErrorToFile("Yaci Store query error for epoch " + epoch, e);
        }

        boolean mismatch = false;
        int mismatchCount = 0; // track number of mismatches for this epoch

        // Compare the results between DB Sync and Indexer.
        logLine("Comparing results:");
        for (Map.Entry<String, BigDecimal> entry : dbSyncResults.entrySet()) {
            String hash = entry.getKey();
            BigDecimal amountDbSync = entry.getValue();

            if (indexerResults.containsKey(hash)) {
                BigDecimal amountIndexer = indexerResults.get(hash);
                if (amountDbSync.compareTo(amountIndexer) == 0) {
                   // System.out.println("Match found for hash: " + hash + " - Amount: " + amountDbSync);
                } else {
                    mismatch = true;
                    mismatchCount++;
                    logLine("Mismatch for hash: " + hash +
                            " - DB Sync Amount: " + amountDbSync + ", Indexer Amount: " + amountIndexer);
                    logLine("DRep Id: " + drepHashToIdMap.get(hash));
                }
            } else {
                mismatch = true;
                mismatchCount++;
                logLine("Hash " + hash + " found in DB Sync but not in Indexer -- amount : " + amountDbSync);
                logLine("DRep Id: " + drepHashToIdMap.get(hash));
            }
        }

        // Optionally, check if there are any hashes in indexer that are missing in DB Sync.
        for (String hash : indexerResults.keySet()) {
            if (!dbSyncResults.containsKey(hash)) {
                mismatch = true;
                mismatchCount++;
                logLine("Hash " + hash + " found in Indexer but not in DB Sync -- amount : " + indexerResults.get(hash));
                logLine("DRep Id: " + drepHashToIdMap.get(hash));
            }
        }

        // Compare abstain amounts
        if (dbSyncAbstainAmount.compareTo(indexerAbstainAmount) == 0) {
            // System.out.println("Match found for abstain amount: " + dbSyncAbstainAmount);
        } else {
            mismatch = true;
            mismatchCount++;
            logLine("Mismatch for abstain amount: DB Sync Amount: " + dbSyncAbstainAmount +
                    ", Indexer Amount: " + indexerAbstainAmount);
        }

        // Compare no confidence amounts
        if (dbSyncNoConfidenceAmount.compareTo(indexerNoConfidenceAmount) == 0) {
            // System.out.println("Match found for no confidence amount: " + dbSyncNoConfidenceAmount);
        } else {
            mismatch = true;
            mismatchCount++;
            logLine("Mismatch for no confidence amount: DB Sync Amount: " + dbSyncNoConfidenceAmount +
                    ", Indexer Amount: " + indexerNoConfidenceAmount);
        }

        if (mismatch) {
            logLine("❌ Mismatches found: " + mismatchCount + " between DB Sync and Indexer results.");
        } else {
            logLine("✅ All results match between DB Sync and Indexer.");
        }
        return mismatchCount;
    }

    public static void main(String[] args) {
        DrepDataComparator comparator = new DrepDataComparator();

        int totalMismatchCount = 0;
        int epochsWithMismatch = 0;
        int totalEpochs = Math.max(0, endEpoch - startEpoch + 1);

        for (int i = startEpoch; i <= endEpoch; i++) {
            logLine("\n############ Comparing drep dist data for epoch: " + i + " ############");
            int epochMismatch = comparator.compareDrepDistData(i);
            if (epochMismatch > 0) epochsWithMismatch++;
            totalMismatchCount += epochMismatch;
            logLine("############ Finished comparing drep dist data for epoch: " + i + " ############");
        }

        logLine("\n===== Comparison Summary =====");
        logLine("Epochs compared: " + totalEpochs);
        logLine("Epochs with mismatches: " + epochsWithMismatch + "/" + totalEpochs);
        logLine("Total mismatches across all epochs: " + totalMismatchCount);

    }

    private static synchronized void logLine(String message) {
        System.out.println(message);
        try {
            Files.write(LOG_FILE, (message + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Failed to write log line: " + e.getMessage());
        }
    }

    private static synchronized void logErrorToFile(String message, Throwable t) {
        System.err.println(message + ": " + t.getMessage());
        try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
            t.printStackTrace(pw);
            String stack = sw.toString();
            Files.write(LOG_FILE,
                    (message + System.lineSeparator() + stack + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ioe) {
            System.err.println("Failed to write error log: " + ioe.getMessage());
        }
    }
}

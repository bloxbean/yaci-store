package com.bloxbean.cardano.yaci.store.test.e2e;

import lombok.extern.slf4j.Slf4j;

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
 * The DrepActiveUntilComparator class is responsible for comparing active_until value (drep)
  between two database systems: DB Sync and Yaci Store database.
 */
@Slf4j
public class DrepActiveUntilComparator {
    static int startEpoch = 740;
    static int endEpoch = 902;

    private static final Path LOG_DIR = Paths.get("logs");
    private static final DateTimeFormatter LOG_TS_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final String RUN_TS = LocalDateTime.now().format(LOG_TS_FORMAT);
    private static final Path LOG_FILE = LOG_DIR.resolve("drep_active_until_compare-" + RUN_TS + ".log");

    static {
        try {
            if (!Files.exists(LOG_DIR)) Files.createDirectories(LOG_DIR);
            if (!Files.exists(LOG_FILE)) Files.createFile(LOG_FILE);
            logLine("===== Start DrepActiveUntil comparison run =====");
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

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static String normalizeHash(String hash) {
        if (hash == null) return null;
        if (hash.startsWith("0x") || hash.startsWith("0X")) hash = hash.substring(2);
        return hash.toLowerCase();
    }

    public static int compareActiveUntilForEpoch(int epoch) {
        String dbSyncQuery = "SELECT dh.raw, d.active_until FROM drep_distr d " +
                "INNER JOIN drep_hash dh ON dh.id = d.hash_id WHERE d.epoch_no = ? and d.active_until is not null";

        String indexerQuery = "SELECT drep_hash, active_until FROM drep_dist WHERE epoch = ? and drep_type not in ('ABSTAIN', 'NO_CONFIDENCE')";

        Map<String, Integer> dbSyncMap = new HashMap<>();
        Map<String, Integer> indexerMap = new HashMap<>();

        boolean mismatch = false;
        int mismatchCount = 0;

        // Fetch from DB Sync
        try (Connection conn = DriverManager.getConnection(dbSyncUrl, dbSyncUser, dbSyncPassword);
             PreparedStatement ps = conn.prepareStatement(dbSyncQuery)) {

            ps.setInt(1, epoch);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                byte[] hashBytes = rs.getBytes("raw");
                String hash = hashBytes != null ? normalizeHash(bytesToHex(hashBytes)) : null;
                Integer activeUntil = rs.getObject("active_until") != null ? rs.getInt("active_until") : null;

                if (hash != null)
                    dbSyncMap.put(hash, activeUntil);
            }
        } catch (SQLException e) {
            log.error("Error while fetching data from DB Sync for epoch {}" , epoch, e);
            logErrorToFile("DB Sync query error for epoch " + epoch, e);
        }

        // Fetch from Yaci Store
        try (Connection conn = DriverManager.getConnection(storeUrl, storeDBUser, storeDBPassword);
             PreparedStatement ps = conn.prepareStatement(indexerQuery)) {

            ps.setInt(1, epoch);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String hash = normalizeHash(rs.getString("drep_hash"));
                Integer activeUntil = rs.getObject("active_until") != null ? rs.getInt("active_until") : null;

                if (hash != null)
                    indexerMap.put(hash, activeUntil);
            }
        } catch (SQLException e) {
            log.error("Error while fetching data from Yaci Store for epoch {}", epoch, e);
            logErrorToFile("Yaci Store query error for epoch " + epoch, e);
        }

        // Compare results
        for (Map.Entry<String, Integer> entry : dbSyncMap.entrySet()) {
            String hash = entry.getKey();
            Integer dbValue = entry.getValue();

            if (indexerMap.containsKey(hash)) {
                Integer storeValue = indexerMap.get(hash);
                if (!equalsNullableInt(dbValue, storeValue)) {
                    mismatch = true;
                    mismatchCount++;
                    logLine("Mismatch for hash: " + hash);
                    logLine("  → DB Sync   : active_until = " + dbValue);
                    logLine("  → Yaci Store: active_until = " + storeValue);
                }
            } else {
                mismatch = true;
                mismatchCount++;
                logLine("Hash " + hash + " found in DB Sync but not in Yaci Store.");
            }
        }

        for (String hash : indexerMap.keySet()) {
            if (!dbSyncMap.containsKey(hash)) {
                mismatch = true;
                mismatchCount++;
                logLine("Hash " + hash + " found in Yaci Store but not in DB Sync.");
            }
        }

        if (mismatch) {
            logLine("❌ Mismatches found: " + mismatchCount + " in active_until for epoch " + epoch);
        } else {
            logLine("✅ All active_until values match for epoch " + epoch);
        }
        return mismatchCount;
    }

    private static boolean equalsNullableInt(Integer a, Integer b) {
        return (a == null && b == null) || (a != null && a.equals(b));
    }

    public static void main(String[] args) {
        int totalMismatchCount = 0;
        int epochsWithMismatch = 0;
        int totalEpochs = Math.max(0, endEpoch - startEpoch + 1);

        for (int i = startEpoch; i <= endEpoch; i++) {
            logLine("\n============ Comparing active_until for epoch: " + i + " ============");
            int epochMismatch = compareActiveUntilForEpoch(i);
            if (epochMismatch > 0) epochsWithMismatch++;
            totalMismatchCount += epochMismatch;
            logLine("============ Finished epoch: " + i + " ============");
        }

        logLine("\n===== Drep ActiveUntil Comparison Summary =====");
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

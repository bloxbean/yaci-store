package com.bloxbean.cardano.yaci.store.test.e2e;

import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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

@Slf4j
public class RewardRestDataComparator {
    static int startEpoch = 1075;
    static int endEpoch = 1080;
    static String rewardType = "proposal_refund"; // e.g., treasury, reserves, proposal_refund

    private static final Path LOG_DIR = Paths.get("logs");
    private static final DateTimeFormatter LOG_TS_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final String RUN_TS = LocalDateTime.now().format(LOG_TS_FORMAT);
    private static final Path LOG_FILE = LOG_DIR.resolve("reward_rest_compare-" + RUN_TS + ".log");

    static {
        try {
            if (!Files.exists(LOG_DIR)) {
                Files.createDirectories(LOG_DIR);
            }
            if (!Files.exists(LOG_FILE)) {
                Files.createFile(LOG_FILE);
            }
            logLine("\n===== Start RewardRest comparison run =====");
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

    public static int compareRewardRestForEpoch(int epoch) {
        String dbSyncQuery = "SELECT sa.view, rr.type, rr.earned_epoch, rr.amount, rr.spendable_epoch " +
                "FROM reward_rest rr " +
                "INNER JOIN stake_address sa ON sa.id = rr.addr_id " +
                "WHERE rr.type = ?::rewardtype and rr.earned_epoch = ? ORDER BY rr.earned_epoch, sa.view, rr.amount";

        String storeQuery = "SELECT address, type, earned_epoch, amount, spendable_epoch " +
                "FROM reward_rest " +
                "WHERE type = ? and earned_epoch = ? ORDER BY earned_epoch, address, amount";

        // Use multiset-style maps keyed by full record to avoid collisions
        Map<RewardRestData, Integer> dbSyncMap = new HashMap<>();
        Map<RewardRestData, Integer> storeMap = new HashMap<>();

        boolean mismatch = false;
        int mismatchCount = 0;

        // Fetch from DB Sync
        try (Connection conn = DriverManager.getConnection(dbSyncUrl, dbSyncUser, dbSyncPassword);
             PreparedStatement ps = conn.prepareStatement(dbSyncQuery)) {

            ps.setString(1, rewardType);
            ps.setInt(2, epoch);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String address = rs.getString("view");
                String type = rs.getString("type");
                Integer earnedEpoch = rs.getInt("earned_epoch");
                BigInteger amount = new BigInteger(rs.getString("amount"));
                Integer spendableEpoch = rs.getObject("spendable_epoch") != null ? rs.getInt("spendable_epoch") : null;

                RewardRestData data = new RewardRestData(address, type, earnedEpoch, amount, spendableEpoch);
                dbSyncMap.merge(data, 1, Integer::sum);
            }
        } catch (SQLException e) {
            log.error("Error while fetching data from DB Sync for epoch {}", epoch, e);
            logErrorToFile("Error while fetching data from DB Sync for epoch " + epoch, e);
        }

        // Fetch from Yaci Store
        try (Connection conn = DriverManager.getConnection(storeUrl, storeDBUser, storeDBPassword);
             PreparedStatement ps = conn.prepareStatement(storeQuery)) {

            ps.setString(1, rewardType);
            ps.setInt(2, epoch);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String address = rs.getString("address");
                String type = rs.getString("type");
                Integer earnedEpoch = rs.getInt("earned_epoch");
                BigInteger amount = rs.getBigDecimal("amount").toBigInteger();
                Integer spendableEpoch = rs.getObject("spendable_epoch") != null ? rs.getInt("spendable_epoch") : null;

                RewardRestData data = new RewardRestData(address, type, earnedEpoch, amount, spendableEpoch);
                storeMap.merge(data, 1, Integer::sum);
            }
        } catch (SQLException e) {
            log.error("Error while fetching data from Yaci Store for epoch {}", epoch, e);
            logErrorToFile("Error while fetching data from Yaci Store for epoch " + epoch, e);
        }

        // Compare results
        for (Map.Entry<RewardRestData, Integer> entry : dbSyncMap.entrySet()) {
            RewardRestData data = entry.getKey();
            int dbCount = entry.getValue();

            Integer storeCount = storeMap.get(data);
            if (storeCount == null) {
                mismatch = true;
                mismatchCount++;
                logLine("Entry present only in DB Sync: " + data + " (count=" + dbCount + ")");
            } else if (!storeCount.equals(dbCount)) {
                mismatch = true;
                mismatchCount++;
                logLine("Count mismatch for: " + data + " → DB Sync=" + dbCount + ", Yaci Store=" + storeCount);
            }
        }

        for (Map.Entry<RewardRestData, Integer> entry : storeMap.entrySet()) {
            RewardRestData data = entry.getKey();
            int storeCount = entry.getValue();
            Integer dbCount = dbSyncMap.get(data);
            if (dbCount == null) {
                mismatch = true;
                mismatchCount++;
                logLine("Entry present only in Yaci Store: " + data + " (count=" + storeCount + ")");
            }
        }

        if (mismatch) {
            logLine("❌ Mismatches found: " + mismatchCount + " in reward_rest for epoch " + epoch + " (type=" + rewardType + ")");
        } else {
            logLine("✅ All reward_rest data matches for epoch " + epoch + " (type=" + rewardType + ")");
        }
        return mismatchCount;
    }

    private static class RewardRestData {
        String address;
        String type;
        Integer earnedEpoch;
        BigInteger amount;
        Integer spendableEpoch;

        public RewardRestData(String address, String type, Integer earnedEpoch, BigInteger amount, Integer spendableEpoch) {
            this.address = address;
            this.type = type;
            this.earnedEpoch = earnedEpoch;
            this.amount = amount;
            this.spendableEpoch = spendableEpoch;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RewardRestData that = (RewardRestData) o;
            return Objects.equals(address, that.address) &&
                    Objects.equals(type, that.type) &&
                    Objects.equals(earnedEpoch, that.earnedEpoch) &&
                    Objects.equals(amount, that.amount) &&
                    Objects.equals(spendableEpoch, that.spendableEpoch);
        }

        @Override
        public int hashCode() {
            return Objects.hash(address, type, earnedEpoch, amount, spendableEpoch);
        }

        @Override
        public String toString() {
            return "RewardRestData{" +
                    "address='" + address + '\'' +
                    ", type='" + type + '\'' +
                    ", earnedEpoch=" + earnedEpoch +
                    ", amount=" + amount +
                    ", spendableEpoch=" + spendableEpoch +
                    '}';
        }
    }

    public static void main(String[] args) {
        if (args != null && args.length > 0 && args[0] != null && !args[0].isEmpty()) {
            rewardType = args[0];
            logLine("Using reward_rest type: " + rewardType);
        } else {
            logLine("Using default reward_rest type: " + rewardType);
        }

        int totalMismatchCount = 0;
        int epochsWithMismatch = 0;
        int totalEpochs = Math.max(0, endEpoch - startEpoch + 1);

        for (int i = startEpoch; i <= endEpoch; i++) {
            logLine("\n============ Comparing reward_rest for epoch: " + i + " (type=" + rewardType + ") ============");
            int epochMismatch = compareRewardRestForEpoch(i);
            if (epochMismatch > 0) epochsWithMismatch++;
            totalMismatchCount += epochMismatch;
            logLine("============ Finished epoch: " + i + " (type=" + rewardType + ") ============");
        }

        logLine("\n===== RewardRest Comparison Summary =====");
        logLine("Epochs compared: " + totalEpochs);
        logLine("Epochs with mismatches: " + epochsWithMismatch + "/" + totalEpochs);
        logLine("Total mismatches across all epochs: " + totalMismatchCount);
    }

    private static synchronized void logLine(String message) {
        System.out.println(message);
        try {
            Files.write(LOG_FILE,
                    (message + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
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

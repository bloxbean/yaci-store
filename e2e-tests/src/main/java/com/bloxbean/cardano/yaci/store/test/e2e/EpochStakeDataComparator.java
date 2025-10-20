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
public class EpochStakeDataComparator {
    static int startEpoch = 740;
    static int endEpoch = 902;

    private static final Path LOG_DIR = Paths.get("logs");
    private static final DateTimeFormatter LOG_TS_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final String RUN_TS = LocalDateTime.now().format(LOG_TS_FORMAT);
    private static final Path LOG_FILE = LOG_DIR.resolve("epoch_stake_compare-" + RUN_TS + ".log");

    static {
        try {
            if (!Files.exists(LOG_DIR)) Files.createDirectories(LOG_DIR);
            if (!Files.exists(LOG_FILE)) Files.createFile(LOG_FILE);
            logLine("===== Start EpochStake comparison run =====");
            logLine("Log file: " + LOG_FILE.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Failed to initialize log file: " + e.getMessage());
        }
    }

    // Connection details for the two databases (update these with your real values)
    static String dbSyncUrl = "jdbc:postgresql://<db_sync_host>:<db_sync_port>/cexplorer?currentSchema=public";
    static String dbSyncUser = "postgres";
    static String dbSyncPassword = "<dbsync_password>";

    static String storeUrl = "jdbc:postgresql://localhost:5433/yaci_indexer?currentSchema=preview";
    static String storeDBUser = "user";
    static String storeDBPassword = "";

    public static void compareEpochStakeForEpoch(int epoch) {
        String dbSyncQuery = "SELECT sa.view, es.amount, encode(ph.hash_raw, 'hex') as pool_id " +
                "FROM epoch_stake es " +
                "INNER JOIN stake_address sa ON sa.id = es.addr_id " +
                "INNER JOIN pool_hash ph ON ph.id = es.pool_id " +
                "WHERE es.epoch_no = ? ORDER BY sa.view, encode(ph.hash_raw, 'hex') , amount";

        String storeQuery = "SELECT address, amount, pool_id " +
                "FROM epoch_stake " +
                "WHERE active_epoch = ? ORDER BY address, pool_id, amount";

        Map<String, EpochStakeData> dbSyncMap = new HashMap<>();
        Map<String, EpochStakeData> storeMap = new HashMap<>();

        boolean mismatch = false;

        // Fetch from DB Sync
        try (Connection conn = DriverManager.getConnection(dbSyncUrl, dbSyncUser, dbSyncPassword);
             PreparedStatement ps = conn.prepareStatement(dbSyncQuery)) {

            ps.setInt(1, epoch);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String address = rs.getString("view");
                BigInteger amount = new BigInteger(rs.getString("amount"));
                String poolId = rs.getString("pool_id");

                String key = address + "_" + poolId;
                dbSyncMap.put(key, new EpochStakeData(address, amount, poolId));
            }
        } catch (SQLException e) {
            log.error("Error while fetching data from DB Sync for epoch {}", epoch, e);
            logErrorToFile("DB Sync query error for epoch " + epoch, e);
        }

        // Fetch from Yaci Store
        try (Connection conn = DriverManager.getConnection(storeUrl, storeDBUser, storeDBPassword);
             PreparedStatement ps = conn.prepareStatement(storeQuery)) {

            ps.setInt(1, epoch);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String address = rs.getString("address");
                BigInteger amount = rs.getBigDecimal("amount").toBigInteger();
                String poolId = rs.getString("pool_id");

                String key = address + "_" + poolId;
                storeMap.put(key, new EpochStakeData(address, amount, poolId));
            }
        } catch (SQLException e) {
            log.error("Error while fetching data from Yaci Store for epoch {}", epoch, e);
            logErrorToFile("Yaci Store query error for epoch " + epoch, e);
        }

        // Compare results
        for (Map.Entry<String, EpochStakeData> entry : dbSyncMap.entrySet()) {
            String key = entry.getKey();
            EpochStakeData dbData = entry.getValue();

            if (storeMap.containsKey(key)) {
                EpochStakeData storeData = storeMap.get(key);
                if (!dbData.equals(storeData)) {
                    mismatch = true;
                    logLine("Mismatch for key: " + key);
                    logLine("  → DB Sync   : " + dbData);
                    logLine("  → Yaci Store: " + storeData);
                }
            } else {
                mismatch = true;
                logLine("Key " + key + " found in DB Sync but not in Yaci Store.");
            }
        }

        for (String key : storeMap.keySet()) {
            if (!dbSyncMap.containsKey(key)) {
                mismatch = true;
                logLine("Key " + key + " found in Yaci Store but not in DB Sync.");
            }
        }

        if (mismatch) {
            logLine("❌ Mismatch found in epoch_stake for epoch " + epoch);
        } else {
            logLine("✅ All epoch_stake data matches for epoch " + epoch);
        }
    }

    private static class EpochStakeData {
        String address;
        BigInteger amount;
        String poolId;

        public EpochStakeData(String address, BigInteger amount, String poolId) {
            this.address = address;
            this.amount = amount;
            this.poolId = poolId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EpochStakeData that = (EpochStakeData) o;
            return Objects.equals(address, that.address) &&
                    Objects.equals(amount, that.amount) &&
                    Objects.equals(poolId, that.poolId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(address, amount, poolId);
        }

        @Override
        public String toString() {
            return "EpochStakeData{" +
                    "address='" + address + '\'' +
                    ", amount=" + amount +
                    ", poolId='" + poolId + '\'' +
                    '}';
        }
    }

    public static void main(String[] args) throws InterruptedException {
        for (int i = startEpoch; i >= endEpoch; i--) {
            logLine("\n============ Comparing epoch_stake for epoch: " + i + " ============");
            compareEpochStakeForEpoch(i);
            logLine("============ Finished epoch: " + i + " ============");
            Thread.sleep(5000); // Sleep for 5 seconds between epochs to avoid overwhelming the DB
        }
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

/*
============ Comparing epoch_stake for epoch: 575 ============
Mismatch for key: stake1ux7pt9adw8z46tgqn2f8fvurrhk325gcm4mf75mkmmxpx6gae9mzv_abacadaba9f12a8b5382fc370e4e7e69421fb59831bb4ecca3a11d9b
  → DB Sync   : EpochStakeData{address='stake1ux7pt9adw8z46tgqn2f8fvurrhk325gcm4mf75mkmmxpx6gae9mzv', amount=20046355289, poolId='abacadaba9f12a8b5382fc370e4e7e69421fb59831bb4ecca3a11d9b'}
  → Yaci Store: EpochStakeData{address='stake1ux7pt9adw8z46tgqn2f8fvurrhk325gcm4mf75mkmmxpx6gae9mzv', amount=20021355289, poolId='abacadaba9f12a8b5382fc370e4e7e69421fb59831bb4ecca3a11d9b'}
❌ Mismatch found in epoch_stake for epoch 575
============ Finished epoch: 575 ============
 */


/*
Comparing epoch_stake for epoch: 574 ============
Mismatch for key: stake1ux7pt9adw8z46tgqn2f8fvurrhk325gcm4mf75mkmmxpx6gae9mzv_abacadaba9f12a8b5382fc370e4e7e69421fb59831bb4ecca3a11d9b
  → DB Sync   : EpochStakeData{address='stake1ux7pt9adw8z46tgqn2f8fvurrhk325gcm4mf75mkmmxpx6gae9mzv', amount=20031715318, poolId='abacadaba9f12a8b5382fc370e4e7e69421fb59831bb4ecca3a11d9b'}
  → Yaci Store: EpochStakeData{address='stake1ux7pt9adw8z46tgqn2f8fvurrhk325gcm4mf75mkmmxpx6gae9mzv', amount=20006715318, poolId='abacadaba9f12a8b5382fc370e4e7e69421fb59831bb4ecca3a11d9b'}
❌ Mismatch found in epoch_stake for epoch 574
============ Finished epoch: 574 ============

 */

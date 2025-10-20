package com.bloxbean.cardano.yaci.store.test.e2e;

import java.math.BigDecimal;
import java.sql.*;
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

public class AdapotDataComparator {
    static int startEpoch = 740;
    static int endEpoch = 902;

    // Connection details for the two databases (update these with your real values)
    static String dbSyncUrl = "jdbc:postgresql://<db_sync_host>:<db_sync_port>/cexplorer?currentSchema=public";
    static String dbSyncUser = "postgres";
    static String dbSyncPassword = "<dbsync_password>";

    static String storeUrl = "jdbc:postgresql://localhost:5433/yaci_indexer?currentSchema=preview";
    static String storeDBUser = "user";
    static String storeDBPassword = "";

    private static final Path LOG_DIR = Paths.get("logs");
    private static final DateTimeFormatter LOG_TS_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final String RUN_TS = LocalDateTime.now().format(LOG_TS_FORMAT);
    private static final Path LOG_FILE = LOG_DIR.resolve("adapot_compare-" + RUN_TS + ".log");

    static {
        try {
            if (!Files.exists(LOG_DIR)) Files.createDirectories(LOG_DIR);
            if (!Files.exists(LOG_FILE)) Files.createFile(LOG_FILE);
            logLine("===== Start Adapot comparison run =====");
            logLine("Log file: " + LOG_FILE.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Failed to initialize log file: " + e.getMessage());
        }
    }

    public static void compareAdapotData(int epoch) {
        String dbSyncQuery = "SELECT treasury, reserves " +
                "FROM ada_pots " +
                "WHERE epoch_no = ? " +
                "ORDER BY slot_no DESC " +
                "LIMIT 1";

        String storeQuery = "SELECT treasury, reserves FROM adapot WHERE epoch = ?";

        BigDecimal dbSyncTreasury = null;
        BigDecimal dbSyncReserves = null;

        BigDecimal storeTreasury = null;
        BigDecimal storeReserves = null;

        try (Connection conn = DriverManager.getConnection(dbSyncUrl, dbSyncUser, dbSyncPassword);
             PreparedStatement ps = conn.prepareStatement(dbSyncQuery)) {

            ps.setInt(1, epoch);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                dbSyncTreasury = rs.getBigDecimal("treasury");
                dbSyncReserves = rs.getBigDecimal("reserves");
            } else {
                logLine("No data found in DB Sync for epoch: " + epoch);
                return;
            }
        } catch (SQLException e) {
            logErrorToFile("DB Sync query error for epoch " + epoch, e);
            return;
        }

        try (Connection conn = DriverManager.getConnection(storeUrl, storeDBUser, storeDBPassword);
             PreparedStatement ps = conn.prepareStatement(storeQuery)) {

            ps.setInt(1, epoch);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                storeTreasury = rs.getBigDecimal("treasury");
                storeReserves = rs.getBigDecimal("reserves");
            } else {
                logLine("No data found in Yaci Store for epoch: " + epoch);
                return;
            }
        } catch (SQLException e) {
            logErrorToFile("Yaci Store query error for epoch " + epoch, e);
            return;
        }

        boolean mismatch = false;
        logLine("Comparing adapot data for epoch " + epoch + ":");

        if (dbSyncTreasury != null && storeTreasury != null) {
            if (dbSyncTreasury.compareTo(storeTreasury) == 0) {
                // Match
            } else {
                mismatch = true;
                logLine("Mismatch in Treasury - DB Sync: " + dbSyncTreasury + ", Yaci Store: " + storeTreasury);
            }
        } else {
            mismatch = true;
            logLine("Treasury data missing for epoch " + epoch);
        }

        if (dbSyncReserves != null && storeReserves != null) {
            if (dbSyncReserves.compareTo(storeReserves) == 0) {
                // Match
            } else {
                mismatch = true;
                logLine("Mismatch in Reserves - DB Sync: " + dbSyncReserves + ", Yaci Store: " + storeReserves);
            }
        } else {
            mismatch = true;
            logLine("Reserves data missing for epoch " + epoch);
        }

        if (mismatch) {
            logLine("❌ There are mismatches in adapot data for epoch " + epoch);
        } else {
            logLine("✅ Adapot data matches for epoch " + epoch);
        }
    }

    public static void main(String[] args) {
        AdapotDataComparator comparator = new AdapotDataComparator();

        for (int i = startEpoch; i <= endEpoch; i++) {
            logLine("\n############ Comparing adapot data for epoch: " + i + " ############");
            comparator.compareAdapotData(i);
            logLine("############ Finished comparing adapot data for epoch: " + i + " ############");
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

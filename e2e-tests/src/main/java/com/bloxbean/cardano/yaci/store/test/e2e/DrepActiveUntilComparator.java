package com.bloxbean.cardano.yaci.store.test.e2e;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * The DrepActiveUntilComparator class is responsible for comparing active_until value (drep)
  between two database systems: DB Sync and Yaci Store database.
 */
public class DrepActiveUntilComparator {
    static int startEpoch = 740;
    static int endEpoch = 902;

    // Connection details for the two databases (update these with your real values)
    static String dbSyncUrl = "jdbc:postgresql://<db_sync_host>:<db_sync_port>/cexplorer?currentSchema=public";
    static String dbSyncUser = "postgres";
    static String dbSyncPassword = "<dbsync_password>";

    static String storeUrl = "jdbc:postgresql://localhost:5433/yaci_indexer?currentSchema=preview";
    static String storeDBUser = "user";
    static String storeDBPassword = "";

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

    public static void compareActiveUntilForEpoch(int epoch) {
        String dbSyncQuery = "SELECT dh.raw, d.active_until FROM drep_distr d " +
                "INNER JOIN drep_hash dh ON dh.id = d.hash_id WHERE d.epoch_no = ? and d.active_until is not null";

        String indexerQuery = "SELECT drep_hash, active_until FROM drep_dist WHERE epoch = ? and drep_type not in ('ABSTAIN', 'NO_CONFIDENCE')";

        Map<String, Integer> dbSyncMap = new HashMap<>();
        Map<String, Integer> indexerMap = new HashMap<>();

        boolean mismatch = false;

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
            e.printStackTrace();
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
            e.printStackTrace();
        }

        // Compare results
        for (Map.Entry<String, Integer> entry : dbSyncMap.entrySet()) {
            String hash = entry.getKey();
            Integer dbValue = entry.getValue();

            if (indexerMap.containsKey(hash)) {
                Integer storeValue = indexerMap.get(hash);
                if (!equalsNullableInt(dbValue, storeValue)) {
                    mismatch = true;
                    System.out.println("Mismatch for hash: " + hash);
                    System.out.println("  → DB Sync   : active_until = " + dbValue);
                    System.out.println("  → Yaci Store: active_until = " + storeValue);
                }
            } else {
                mismatch = true;
                System.out.println("Hash " + hash + " found in DB Sync but not in Yaci Store.");
            }
        }

        for (String hash : indexerMap.keySet()) {
            if (!dbSyncMap.containsKey(hash)) {
                mismatch = true;
                System.out.println("Hash " + hash + " found in Yaci Store but not in DB Sync.");
            }
        }

        if (mismatch) {
            System.out.println("❌ Mismatch found in active_until for epoch " + epoch);
        } else {
            System.out.println("✅ All active_until values match for epoch " + epoch);
        }
    }

    private static boolean equalsNullableInt(Integer a, Integer b) {
        return (a == null && b == null) || (a != null && a.equals(b));
    }

    public static void main(String[] args) {
        for (int i = startEpoch; i <= endEpoch; i++) {
            System.out.println("\n============ Comparing active_until for epoch: " + i + " ============");
            compareActiveUntilForEpoch(i);
            System.out.println("============ Finished epoch: " + i + " ============");
        }
    }
}

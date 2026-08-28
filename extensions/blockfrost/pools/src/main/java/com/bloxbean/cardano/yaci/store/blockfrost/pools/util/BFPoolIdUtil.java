package com.bloxbean.cardano.yaci.store.blockfrost.pools.util;

import com.bloxbean.cardano.yaci.store.common.util.PoolUtil;

public final class BFPoolIdUtil {

    private BFPoolIdUtil() {
    }

    public static String toHex(String poolId) {
        if (poolId == null) {
            throw new IllegalArgumentException("Pool ID must not be null");
        }
        if (poolId.length() == 56 && poolId.matches("[0-9a-fA-F]+")) {
            return poolId;
        }
        if (poolId.startsWith(PoolUtil.POOL_ID_PREFIX)) {
            try {
                return PoolUtil.getPoolHash(poolId);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid bech32 pool ID: " + poolId, e);
            }
        }
        throw new IllegalArgumentException("Invalid pool ID format: " + poolId);
    }

    public static boolean isValid(String poolId) {
        try {
            toHex(poolId);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static String normalizeOrder(String order) {
        if (order == null || order.isBlank()) {
            return "asc";
        }
        String lower = order.trim().toLowerCase();
        if ("desc".equals(lower)) {
            return "desc";
        }
        return "asc";
    }
}

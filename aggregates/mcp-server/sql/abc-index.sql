-- Needs indexes in @indexe_address_balance_current.sql

-- 1) Top-N addresses holding a particular unit (policyId||assetNameHex or 'lovelace')
--    Query this way:
--    SELECT address, quantity
--    FROM yaci_store.address_balance_current
--    WHERE unit = $1 AND quantity > 0
--    ORDER BY quantity DESC
--    LIMIT 10;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_abc_unit_qty_desc
    ON address_balance_current (unit, quantity DESC)
    INCLUDE (address)
    WHERE quantity > 0;


-- 2) Holders under a policy (prefix = first 56 hex chars of unit)
--    a) List all distinct holders for a policy
--       SELECT DISTINCT address
--       FROM yaci_store.address_balance_current
--       WHERE LEFT(unit,56) = $1 AND quantity > 0;
--
--    b) Rank addresses by #tokens (useful for NFT distributions)
--       SELECT address, COUNT(*) AS tokens_held
--       FROM yaci_store.address_balance_current
--       WHERE LEFT(unit,56) = $1 AND quantity > 0
--       GROUP BY address
--       ORDER BY tokens_held DESC
--       LIMIT 10;
--
--    c) Top addresses by total quantity across all assets under the policy
--       SELECT address, SUM(quantity) AS total_qty
--       FROM yaci_store.address_balance_current
--       WHERE LEFT(unit,56) = $1 AND quantity > 0
--       GROUP BY address
--       ORDER BY total_qty DESC
--       LIMIT 10;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_abc_policy_prefix_addr
    ON address_balance_current (LEFT(unit,56), address)
    INCLUDE (quantity)
    WHERE quantity > 0;


-- 3) (Optional) If you often browse holders within a policy ordered by per-row quantity
--    (not aggregated), this helps WHERE LEFT(unit,56) = $1 ORDER BY quantity DESC queries.
-- CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_abc_policy_prefix_qty_desc
--     ON address_balance_current (LEFT(unit,56), quantity DESC)
--     INCLUDE (address)
--     WHERE quantity > 0;


-- 4) LIKE 'policyid%' instead of LEFT(unit,56)=...
-- CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_abc_unit_like
--     ON address_balance_current (unit varchar_pattern_ops);


-- ============================================
-- OPTIONAL: ADDRESS BALANCE HISTORY INDEX
-- ============================================

-- 5) Historical address balance by epoch (OPTIONAL - may improve epoch queries)
--    Query pattern:
--    SELECT quantity, slot, block_time
--    FROM address_balance
--    WHERE address = $1 AND unit = $2 AND epoch = $3
--    ORDER BY slot DESC
--    LIMIT 1;
--
--    Note: PK (address, unit, slot) already covers slot-based queries efficiently.
--    This index is only useful if you frequently query by epoch instead of slot.

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_ab_address_unit_epoch
    ON address_balance (address, unit, epoch, slot DESC);

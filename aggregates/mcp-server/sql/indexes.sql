-- ============================================
-- MCP Server Performance Indexes
-- ============================================
-- Created: 2025-10-16
-- Purpose: Optimize aggregation queries for MCP server tools
--
-- IMPORTANT: Run these indexes manually using CONCURRENTLY to avoid table locks
-- Example: psql -h localhost -p 5432 -U postgres -d yaci_store < indexes.sql
--
-- These indexes were carefully analyzed to avoid redundancy
-- Only 8 critical missing indexes are included (out of 33 analyzed queries)
-- ============================================

-- Set schema (adjust if needed)
-- SET search_path TO preprod2;

-- ============================================
-- HIGH PRIORITY INDEXES - CREATE IMMEDIATELY
-- ============================================

-- --------------------------------------------
-- tx_input table indexes
-- --------------------------------------------

-- Index 1: For joining tx_input with transaction table
-- Used in: Script analytics queries, net transfer queries
-- Impact: 7+ queries, critical for JOIN performance
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_tx_input_spent_tx_hash
    ON tx_input (spent_tx_hash);

-- Index 2: For epoch-based analytics
-- Used in: Script fee analytics, historical balance queries
-- Impact: 6+ queries, critical for epoch range filtering
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_tx_input_spent_epoch
    ON tx_input (spent_epoch);

-- Index 3: Covering index for historical balance EXISTS subqueries
-- Used in: Balance at epoch, balance timeline queries
-- Impact: 5+ queries, eliminates table lookups in EXISTS checks
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_tx_input_utxo_spent_epoch
    ON tx_input (tx_hash, output_index, spent_epoch);

-- --------------------------------------------
-- transaction table indexes
-- --------------------------------------------

-- Index 4: For filtered epoch aggregations
-- Used in: Epoch transaction stats, fee distribution analysis
-- Impact: 3+ queries, improves GROUP BY epoch with invalid filter
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_transaction_epoch_invalid
    ON transaction (epoch, invalid);

-- Index 5: For recent transaction queries with slot range (partial index)
-- Used in: Net transfer batch queries, recent transaction lists
-- Impact: 2+ critical queries, 10-20x performance improvement expected
-- Note: Partial index for space efficiency (only valid transactions)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_transaction_slot_invalid_desc
    ON transaction (slot DESC, invalid)
    WHERE invalid = false;

-- --------------------------------------------
-- reward table index
-- --------------------------------------------

-- Index 6: For pool reward queries
-- Used in: Pool rewards summary, top reward pools, pool complete stats
-- Impact: 4+ queries, critical for pool reward aggregations
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_reward_pool_id_earned_epoch
    ON reward (pool_id, earned_epoch);

-- --------------------------------------------
-- address_utxo table indexes
-- --------------------------------------------

-- Index 7: For historical balance queries
-- Used in: Balance at epoch, balance timeline, asset balance at epoch
-- Impact: 5+ queries, 3-5x performance improvement expected
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_address_utxo_owner_addr_epoch
    ON address_utxo (owner_addr, epoch);

-- Index 8: For precise slot-based historical queries
-- Used in: Balance at slot, balance summary with slot filtering
-- Impact: 2+ queries, important for intra-epoch precision
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_address_utxo_owner_addr_slot
    ON address_utxo (owner_addr, slot);

-- ============================================
-- OPTIONAL INDEXES - VERIFY NEED FIRST
-- ============================================
-- Uncomment and create only if query performance analysis shows they're needed
-- Use EXPLAIN ANALYZE on affected queries to verify benefit before creating

-- --------------------------------------------
-- block table index (optional)
-- --------------------------------------------

-- Optional Index 9: For pool performance queries by epoch
-- Used in: Pool block production stats
-- Impact: 2 queries
-- Verification: Check if existing idx_block_slot_leader is sufficient
-- CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_block_slot_leader_epoch
--     ON block (slot_leader, epoch);

-- --------------------------------------------
-- epoch_stake table index (optional)
-- --------------------------------------------

-- Optional Index 10: For pool-first queries
-- Used in: Pool delegator stats
-- Impact: 2 queries
-- Verification: Check if idx_epoch_stake_active_epoch_pool_id is used efficiently
-- Note: May be redundant with existing composite index (active_epoch, pool_id)
-- CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_epoch_stake_pool_id_active_epoch
--     ON epoch_stake (pool_id, active_epoch);

-- --------------------------------------------
-- address_utxo table index (optional)
-- --------------------------------------------

-- Optional Index 11: For stake address portfolio queries
-- Used in: Stake address portfolio queries
-- Impact: 3 queries
-- Verification: Check if single-column idx_address_utxo_owner_stake_addr is sufficient
-- CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_address_utxo_owner_stake_addr_slot
--     ON address_utxo (owner_stake_addr, slot);

-- --------------------------------------------
-- assets table index (optional)
-- --------------------------------------------

-- Optional Index 12: For recent mint queries with quantity filter (partial index)
-- Used in: Find recent token mints
-- Impact: 1 query
-- Verification: Check if existing idx_assets_slot is sufficient
-- CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_assets_slot_desc_quantity
--     ON assets (slot DESC, quantity)
--     WHERE quantity > 0;

-- ============================================
-- POST-CREATION VERIFICATION
-- ============================================

-- After creating indexes, verify they are being used:
--
-- 1. Check index usage statistics:
-- SELECT schemaname, tablename, indexname, idx_scan, idx_tup_read, idx_tup_fetch
-- FROM pg_stat_user_indexes
-- WHERE schemaname = 'preprod2'
-- AND indexname LIKE 'idx_%'
-- ORDER BY idx_scan DESC;
--
-- 2. Check index sizes:
-- SELECT schemaname, tablename, indexname,
--        pg_size_pretty(pg_relation_size(indexrelid)) as index_size
-- FROM pg_stat_user_indexes
-- WHERE schemaname = 'preprod2'
-- AND indexname LIKE 'idx_%'
-- ORDER BY pg_relation_size(indexrelid) DESC;
--
-- 3. Test critical queries with EXPLAIN ANALYZE:
-- EXPLAIN ANALYZE <your query here>;
--
-- Look for "Index Scan" or "Index Only Scan" in the execution plan
-- If you see "Seq Scan", the index may not be optimal for that query

-- ============================================
-- EXPECTED PERFORMANCE IMPROVEMENTS
-- ============================================
--
-- Script analytics queries: 5-10x faster (indexes 1, 2, 3)
-- Transaction net transfer batch: 10-20x faster (indexes 5, 1)
-- Historical balance queries: 3-5x faster (indexes 3, 7, 8)
-- Pool rewards queries: 3-5x faster (index 6)
-- Epoch aggregations: 2-3x faster (index 4)
--
-- ============================================
-- MAINTENANCE NOTES
-- ============================================
--
-- 1. Monitor unused indexes after 1 week:
--    - If idx_scan = 0, consider dropping the index
--
-- 2. Index bloat:
--    - Run REINDEX CONCURRENTLY if indexes grow too large
--
-- 3. Query plan changes:
--    - PostgreSQL may need statistics updated: ANALYZE <table_name>
--
-- 4. For large tables:
--    - Creating indexes may take significant time
--    - CONCURRENTLY allows queries to continue during creation
--    - Monitor with: SELECT * FROM pg_stat_progress_create_index;

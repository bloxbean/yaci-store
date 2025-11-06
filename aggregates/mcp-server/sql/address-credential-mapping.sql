-- ============================================
-- Address Credential Mapping Materialized View
-- ============================================
-- Purpose: Enable fast payment credential lookups for contract TVL
-- and Franken address aggregation
--
-- Usage: Required for optimized contract-tvl-estimation tool
-- Refresh: Every epoch or on-demand
-- ============================================

-- Create materialized view
CREATE MATERIALIZED VIEW IF NOT EXISTS address_credential_mapping AS
SELECT DISTINCT
    owner_addr as address,
    owner_payment_credential as payment_credential,
    owner_stake_credential as stake_credential,
    owner_stake_addr as stake_address
FROM address_utxo
WHERE owner_addr IS NOT NULL
  AND owner_payment_credential IS NOT NULL;

-- Create indexes for optimal query performance
CREATE UNIQUE INDEX IF NOT EXISTS idx_acm_address
    ON address_credential_mapping(address);

CREATE INDEX IF NOT EXISTS idx_acm_payment_cred
    ON address_credential_mapping(payment_credential);

CREATE INDEX IF NOT EXISTS idx_acm_stake_cred
    ON address_credential_mapping(stake_credential);

CREATE INDEX IF NOT EXISTS idx_acm_payment_stake
    ON address_credential_mapping(payment_credential, stake_credential);

-- ============================================
-- Refresh Commands
-- ============================================

-- Manual refresh (development)
-- REFRESH MATERIALIZED VIEW CONCURRENTLY address_credential_mapping;

-- Scheduled refresh (production - via cron)
-- 0 0 */5 * * psql -d yaci_store -c "REFRESH MATERIALIZED VIEW CONCURRENTLY address_credential_mapping;"

-- ============================================
-- Monitoring
-- ============================================

-- Check view size
-- SELECT
--     pg_size_pretty(pg_relation_size('address_credential_mapping')) as view_size,
--     pg_size_pretty(pg_total_relation_size('address_credential_mapping')) as total_with_indexes;

-- Check row count
-- SELECT COUNT(*) as address_count FROM address_credential_mapping;

-- Check last refresh (requires pg_stat_user_tables)
-- SELECT
--     schemaname,
--     matviewname,
--     last_vacuum as last_refresh
-- FROM pg_stat_user_tables
-- WHERE schemaname = current_schema()
--   AND relname = 'address_credential_mapping';

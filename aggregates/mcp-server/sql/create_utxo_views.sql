-- ============================================================================
-- Flattened UTXO Views for Enhanced Dynamic Query Support
-- ============================================================================
-- Purpose: Simplify JSONB queries by flattening the amounts array into
--          standard SQL columns. This makes it easier for LLMs to query
--          asset holdings without complex JSONB operators.
--
-- Usage: Execute these statements manually on your target schema
--        (e.g., preprod, mainnet)
--
-- Note: These are non-materialized views. Consider materializing if
--       performance becomes an issue.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- View 1: utxo_assets_flat
-- ----------------------------------------------------------------------------
-- Description: Core flattened view that expands the JSONB amounts array into
--              individual rows, one per asset per UTXO. Includes spent status.
--
-- Use Cases:
--   - Find UTXOs by policy_id, asset_name, or unit
--   - Filter by spent/unspent status
--   - Query asset holdings by address
--   - Aggregate token quantities
--
-- Example Queries:
--   -- Find all UTXOs for a specific token
--   SELECT * FROM utxo_assets_flat
--   WHERE policy_id = 'abc123...' AND is_spent = false;
--
--   -- Find all assets held by an address
--   SELECT policy_id, asset_name, SUM(quantity)
--   FROM utxo_assets_flat
--   WHERE owner_addr = 'addr1...' AND is_spent = false
--   GROUP BY policy_id, asset_name;
-- ----------------------------------------------------------------------------

DROP VIEW IF EXISTS utxo_assets_flat CASCADE;

CREATE VIEW utxo_assets_flat AS
SELECT
    -- UTXO identification
    au.tx_hash,
    au.output_index,

    -- Owner information
    au.owner_addr,
    au.owner_stake_addr,
    au.owner_payment_credential,
    au.owner_stake_credential,

    -- ADA amount (lovelace)
    au.lovelace_amount,

    -- Block/time information
    au.epoch,
    au.slot,
    au.block,
    au.block_hash,
    au.block_time,
    au.update_datetime,

    -- Flattened asset fields from JSONB
    (asset->>'unit') as asset_unit,
    (asset->>'policy_id') as policy_id,
    (asset->>'asset_name') as asset_name,
    (asset->>'quantity')::numeric as quantity,

    -- Spent status (pre-computed via LEFT JOIN)
    CASE
        WHEN ti.tx_hash IS NULL THEN false
        ELSE true
    END as is_spent
FROM address_utxo au
CROSS JOIN LATERAL jsonb_array_elements(au.amounts) as asset
LEFT JOIN tx_input ti
    ON ti.tx_hash = au.tx_hash
    AND ti.output_index = au.output_index
WHERE (asset->>'unit') <> 'lovelace';  -- Exclude lovelace (use lovelace_amount instead)

COMMENT ON VIEW utxo_assets_flat IS
'Flattened view of address_utxo with JSONB amounts expanded into rows. Each row represents one asset in one UTXO. Includes spent status.';


-- ----------------------------------------------------------------------------
-- View 2: utxo_assets_unspent
-- ----------------------------------------------------------------------------
-- Description: Pre-filtered view containing only unspent UTXOs. This is the
--              most commonly used view since most queries are for current
--              holdings.
--
-- Use Cases:
--   - Current token holdings
--   - Active asset balances
--   - Token holder analysis
--   - Supply calculations
--
-- Example Queries:
--   -- Count holders of a token
--   SELECT COUNT(DISTINCT owner_addr) as holders
--   FROM utxo_assets_unspent
--   WHERE policy_id = 'abc123...';
--
--   -- Find tokens held by multiple addresses
--   SELECT policy_id, asset_name, COUNT(DISTINCT owner_addr) as holders
--   FROM utxo_assets_unspent
--   GROUP BY policy_id, asset_name
--   HAVING COUNT(DISTINCT owner_addr) > 100;
-- ----------------------------------------------------------------------------

DROP VIEW IF EXISTS utxo_assets_unspent CASCADE;

CREATE VIEW utxo_assets_unspent AS
SELECT
    tx_hash,
    output_index,
    owner_addr,
    owner_stake_addr,
    owner_payment_credential,
    owner_stake_credential,
    lovelace_amount,
    epoch,
    slot,
    block,
    block_hash,
    block_time,
    update_datetime,
    asset_unit,
    policy_id,
    asset_name,
    quantity
FROM utxo_assets_flat
WHERE is_spent = false;

COMMENT ON VIEW utxo_assets_unspent IS
'Filtered view of utxo_assets_flat showing only unspent UTXOs. Use this for current holdings and active balances.';


-- ----------------------------------------------------------------------------
-- View 3: token_holder_summary
-- ----------------------------------------------------------------------------
-- Description: Pre-aggregated statistics for each token, including holder
--              count, total supply, and quantity distribution. Great for
--              discovering popular tokens.
--
-- Use Cases:
--   - Find tokens with N+ holders
--   - Analyze token distribution
--   - Token discovery and ranking
--   - Supply analysis
--
-- Example Queries:
--   -- Find tokens with at least 100 holders
--   SELECT * FROM token_holder_summary
--   WHERE holder_count >= 100
--   ORDER BY holder_count DESC;
--
--   -- Find widely distributed tokens
--   SELECT * FROM token_holder_summary
--   WHERE holder_count >= 1000
--     AND avg_quantity < 1000
--   ORDER BY holder_count DESC;
-- ----------------------------------------------------------------------------

DROP VIEW IF EXISTS token_holder_summary CASCADE;

CREATE VIEW token_holder_summary AS
SELECT
    policy_id,
    asset_name,
    asset_unit,
    COUNT(DISTINCT owner_addr) as holder_count,
    SUM(quantity) as total_supply,
    COUNT(*) as utxo_count,
    MIN(quantity) as min_quantity,
    MAX(quantity) as max_quantity,
    AVG(quantity) as avg_quantity,
    PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY quantity) as median_quantity
FROM utxo_assets_unspent
GROUP BY policy_id, asset_name, asset_unit;

COMMENT ON VIEW token_holder_summary IS
'Aggregated statistics per token including holder counts, supply, and quantity distribution. Based on unspent UTXOs only.';


-- ----------------------------------------------------------------------------
-- View 4: address_lovelace_balance
-- ----------------------------------------------------------------------------
-- Description: Simplified view for ADA-only balance queries. No JSONB
--              processing overhead, faster for pure lovelace queries.
--
-- Use Cases:
--   - ADA balance queries
--   - Address balance tracking
--   - Lovelace-only operations
--
-- Example Queries:
--   -- Get current ADA balance for an address
--   SELECT owner_addr, SUM(lovelace_amount) as total_ada
--   FROM address_lovelace_balance
--   WHERE owner_addr = 'addr1...' AND is_spent = false
--   GROUP BY owner_addr;
--
--   -- Track balance history by epoch
--   SELECT epoch, SUM(lovelace_amount) as balance
--   FROM address_lovelace_balance
--   WHERE owner_addr = 'addr1...' AND is_spent = false
--   GROUP BY epoch
--   ORDER BY epoch;
-- ----------------------------------------------------------------------------

DROP VIEW IF EXISTS address_lovelace_balance CASCADE;

CREATE VIEW address_lovelace_balance AS
SELECT
    au.owner_addr,
    au.owner_stake_addr,
    au.owner_payment_credential,
    au.owner_stake_credential,
    au.tx_hash,
    au.output_index,
    au.lovelace_amount,
    au.epoch,
    au.slot,
    au.block,
    au.block_time,
    au.update_datetime,
    CASE
        WHEN ti.tx_hash IS NULL THEN false
        ELSE true
    END as is_spent
FROM address_utxo au
LEFT JOIN tx_input ti
    ON ti.tx_hash = au.tx_hash
    AND ti.output_index = au.output_index;

COMMENT ON VIEW address_lovelace_balance IS
'Simplified view for ADA-only balance queries. Includes spent status but no asset details.';


-- ============================================================================
-- Verification Queries
-- ============================================================================
-- Run these to verify the views were created successfully:
--
-- -- List all views
-- SELECT table_name FROM information_schema.views
-- WHERE table_schema = current_schema()
--   AND table_name LIKE '%utxo%'
-- ORDER BY table_name;
--
-- -- Test utxo_assets_flat
-- SELECT * FROM utxo_assets_flat LIMIT 10;
--
-- -- Test utxo_assets_unspent
-- SELECT * FROM utxo_assets_unspent LIMIT 10;
--
-- -- Test token_holder_summary
-- SELECT * FROM token_holder_summary
-- WHERE holder_count >= 100
-- ORDER BY holder_count DESC
-- LIMIT 10;
--
-- -- Test address_lovelace_balance
-- SELECT * FROM address_lovelace_balance
-- WHERE is_spent = false
-- LIMIT 10;
-- ============================================================================

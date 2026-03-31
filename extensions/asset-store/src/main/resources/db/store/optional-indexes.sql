-- Optional indexes for the asset-store extension.
-- These are NOT applied automatically. Apply them manually if your queries need them.
-- See also: store.auto-index-management in yaci-store for the core index management pattern.

-- CIP-26: lookup by policy ID (for findByPolicy)
CREATE INDEX IF NOT EXISTS idx_ft_offchain_metadata_policy ON ft_offchain_metadata(policy);

-- CIP-26: search by name (for searchByName)
-- Note: LIKE/ILIKE queries benefit from pg_trgm extension + GIN index on PostgreSQL.
-- For basic ILIKE, a btree index on lower(name) can help:
-- CREATE INDEX IF NOT EXISTS idx_ft_offchain_metadata_name_lower ON ft_offchain_metadata(lower(name));

-- CIP-26: lookup by ticker (for findByTicker)
CREATE INDEX IF NOT EXISTS idx_ft_offchain_metadata_ticker ON ft_offchain_metadata(ticker);

-- CIP-26: GIN index on JSONB properties (PostgreSQL only, for custom property queries)
-- CREATE INDEX IF NOT EXISTS idx_ft_offchain_metadata_properties ON ft_offchain_metadata USING GIN(properties);

-- CIP-68: The composite PK (policy_id, asset_name, slot) already covers:
--   - findFirstByPolicyIdAndAssetNameOrderBySlotDesc (exact match on first two columns + sort on third)
--   - findByPolicyId (prefix scan on first column)
--   - findByPolicyIdAndAssetNameOrderBySlotDesc (history query)
-- A standalone slot index is included in the mandatory migration for rollback.
-- No additional indexes should be needed for CIP-68.

-- CIP-113: policy + slot descending (for latest-by-policy queries, only useful when CIP-113 is enabled)
CREATE INDEX IF NOT EXISTS idx_cip113_policy_slot ON cip113_registry_node(policy_id, slot DESC);

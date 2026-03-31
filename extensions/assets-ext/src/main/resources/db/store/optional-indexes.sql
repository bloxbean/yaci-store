-- Optional indexes for the assets-ext extension.
-- These are NOT applied automatically. Apply them manually if your queries need them.

-- CIP-26: case-insensitive name search (for searchByName with ILIKE)
-- Note: pg_trgm extension + GIN index is better for LIKE queries on PostgreSQL.
-- CREATE INDEX IF NOT EXISTS idx_ft_offchain_metadata_name_trgm ON ft_offchain_metadata USING GIN(name gin_trgm_ops);
-- For basic ILIKE without pg_trgm:
-- CREATE INDEX IF NOT EXISTS idx_ft_offchain_metadata_name_lower ON ft_offchain_metadata(lower(name));

-- CIP-26: GIN index on JSONB properties (PostgreSQL only, for custom property queries)
-- CREATE INDEX IF NOT EXISTS idx_ft_offchain_metadata_properties ON ft_offchain_metadata USING GIN(properties);

-- CIP-113: policy + slot descending (for latest-by-policy queries, only useful when CIP-113 is enabled)
CREATE INDEX IF NOT EXISTS idx_cip113_policy_slot ON cip113_registry_node(policy_id, slot DESC);

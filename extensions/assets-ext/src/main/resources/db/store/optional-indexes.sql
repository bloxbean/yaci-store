-- Optional indexes for the assets-ext extension.
-- These are NOT applied automatically during sync. Apply after initial sync reaches chain tip,
-- or use the yaci-store admin CLI tool.

-- CIP-26: lookup by policy ID (for findByPolicy via StorageReader)
CREATE INDEX IF NOT EXISTS idx_ft_offchain_metadata_policy ON ft_offchain_metadata(policy);

-- CIP-26: lookup by ticker (for findByTicker via StorageReader)
CREATE INDEX IF NOT EXISTS idx_ft_offchain_metadata_ticker ON ft_offchain_metadata(ticker);

-- CIP-26: case-insensitive name search (for searchByName with ILIKE)
-- pg_trgm extension + GIN index is best for LIKE queries on PostgreSQL:
-- CREATE INDEX IF NOT EXISTS idx_ft_offchain_metadata_name_trgm ON ft_offchain_metadata USING GIN(name gin_trgm_ops);

-- CIP-26: GIN index on JSONB properties (PostgreSQL only)
-- CREATE INDEX IF NOT EXISTS idx_ft_offchain_metadata_properties ON ft_offchain_metadata USING GIN(properties);

-- CIP-68: label-filtered lookups by policy (for findByPolicyIdAndLabel, findLatestByPolicyIdsAndLabel)
CREATE INDEX IF NOT EXISTS idx_metadata_reference_nft_policy_label ON metadata_reference_nft(policy_id, label, slot DESC);

-- CIP-113: no additional index needed — PK (policy_id, slot, tx_hash) supports backward scan
-- for findFirstByPolicyIdOrderBySlotDesc and findLatestByPolicyIds queries.

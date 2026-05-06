-- Optional indexes for the assets-ext extension.
-- These are NOT applied automatically during sync. Apply after initial sync reaches chain tip,
-- or use the yaci-store admin CLI tool.

-- CIP-26: lookup by ticker (for findByTicker via StorageReader)
CREATE INDEX IF NOT EXISTS idx_cip26_metadata_ticker ON cip26_metadata(ticker);

-- CIP-26: case-insensitive name search (for searchByName with ILIKE)
-- pg_trgm extension + GIN index is best for LIKE queries on PostgreSQL:
-- CREATE INDEX IF NOT EXISTS idx_cip26_metadata_name_trgm ON cip26_metadata USING GIN(name gin_trgm_ops);

-- CIP-26: GIN index on JSONB properties (PostgreSQL only)
-- CREATE INDEX IF NOT EXISTS idx_cip26_metadata_properties ON cip26_metadata USING GIN(properties);

-- CIP-68: label-filtered lookups by policy (for findLatestByConcatenatedKeys; supports
-- index-only scans of the (policy_id, label, slot DESC) prefix used by findFirst*OrderBySlotDesc
-- and the ROW_NUMBER() OVER (PARTITION BY policy_id, asset_name ORDER BY slot DESC) windowing).
CREATE INDEX IF NOT EXISTS idx_cip68_metadata_policy_label ON cip68_metadata(policy_id, label, slot DESC);

-- CIP-68 NFT image / collection-attribute lookups via the JSONB column (PostgreSQL only):
-- CREATE INDEX IF NOT EXISTS idx_cip68_metadata_properties ON cip68_metadata USING GIN(properties);

-- CIP-113: no additional index needed — PK (key, slot, tx_hash) supports backward scan
-- for findFirstByKeyOrderBySlotDesc and findLatestByKeys queries.

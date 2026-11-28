-- assets-ext: tokens that break their CIP are indexed and reported, not dropped (see docs/adr, ADR 0006).
-- validation_errors holds the list of {cip, sub_standard, field_name, field_category, code, error}; NULL until computed.
ALTER TABLE cip26_metadata ADD COLUMN IF NOT EXISTS validation_errors TEXT;
ALTER TABLE cip68_metadata ADD COLUMN IF NOT EXISTS validation_errors TEXT;

-- The CIP limits on these values are reported as validation errors, so the columns must hold what is on-chain
-- or in the registry. A datum is at most one transaction (16 KB), so these stay bounded.
ALTER TABLE cip26_metadata ALTER COLUMN name        TEXT;
ALTER TABLE cip26_metadata ALTER COLUMN ticker      TEXT;
ALTER TABLE cip26_metadata ALTER COLUMN url         TEXT;
ALTER TABLE cip26_metadata ALTER COLUMN description TEXT;
ALTER TABLE cip68_metadata ALTER COLUMN name        TEXT;
ALTER TABLE cip68_metadata ALTER COLUMN ticker      TEXT;
ALTER TABLE cip68_metadata ALTER COLUMN url         TEXT;
ALTER TABLE cip68_metadata ALTER COLUMN media_type  TEXT;

-- A version that is not an integer in the range of a long (or that the CIP does not define) is reported, not dropped.
ALTER TABLE cip68_metadata ALTER COLUMN version SET NULL;

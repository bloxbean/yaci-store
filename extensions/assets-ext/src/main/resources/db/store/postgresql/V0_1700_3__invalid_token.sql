-- assets-ext: what is dropped or unusable and has no token row to carry validation_errors (ADR 0007).
-- One row per dropped item. The id is a SHA-256 of what identifies the item, so a replayed block or a re-run sync never duplicates a row.
CREATE TABLE IF NOT EXISTS invalid_token (
    id             VARCHAR(64)  PRIMARY KEY,
    standard       VARCHAR(16)  NOT NULL,
    combined_with  VARCHAR(16),
    reason_code    VARCHAR(64)  NOT NULL,
    reason         TEXT         NOT NULL,
    policy_id      VARCHAR(56),
    asset_name     VARCHAR(64),
    subject        VARCHAR(256),
    slot           BIGINT,
    tx_hash        VARCHAR(64),
    tx_index       INTEGER,
    output_index   INTEGER,
    origin         VARCHAR(512),
    details        JSONB,
    first_seen_at  TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_invalid_token_slot   ON invalid_token(slot);
CREATE INDEX IF NOT EXISTS idx_invalid_token_reason ON invalid_token(standard, reason_code);
CREATE INDEX IF NOT EXISTS idx_invalid_token_policy ON invalid_token(policy_id);

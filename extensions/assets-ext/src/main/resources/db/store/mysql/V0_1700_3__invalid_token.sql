-- assets-ext: what is dropped or unusable and has no token row to carry validation_errors (ADR 0007).
-- One row per dropped item. The id is a SHA-256 of what identifies the item, so a replayed block or a re-run sync never duplicates a row.
CREATE TABLE invalid_token (
    id             VARCHAR(64)  PRIMARY KEY, -- NOSONAR: plsql:VarcharUsageCheck, VARCHAR is right on MySQL
    standard       VARCHAR(16)  NOT NULL, -- NOSONAR: plsql:VarcharUsageCheck, VARCHAR is right on MySQL
    combined_with  VARCHAR(16), -- NOSONAR: plsql:VarcharUsageCheck, VARCHAR is right on MySQL
    reason_code    VARCHAR(64)  NOT NULL, -- NOSONAR: plsql:VarcharUsageCheck, VARCHAR is right on MySQL
    reason         TEXT         NOT NULL,
    policy_id      VARCHAR(56), -- NOSONAR: plsql:VarcharUsageCheck, VARCHAR is right on MySQL
    asset_name     VARCHAR(64), -- NOSONAR: plsql:VarcharUsageCheck, VARCHAR is right on MySQL
    subject        VARCHAR(256), -- NOSONAR: plsql:VarcharUsageCheck, VARCHAR is right on MySQL
    slot           BIGINT,
    tx_hash        VARCHAR(64), -- NOSONAR: plsql:VarcharUsageCheck, VARCHAR is right on MySQL
    tx_index       INTEGER,
    output_index   INTEGER,
    origin         VARCHAR(512), -- NOSONAR: plsql:VarcharUsageCheck, VARCHAR is right on MySQL
    details        JSON,
    first_seen_at  TIMESTAMP NULL
);

CREATE INDEX idx_invalid_token_slot   ON invalid_token(slot);
CREATE INDEX idx_invalid_token_reason ON invalid_token(standard, reason_code);
CREATE INDEX idx_invalid_token_policy ON invalid_token(policy_id);

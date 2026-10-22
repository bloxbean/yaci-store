-- assets-ext: the CIP-68 fungible (333) and rich fungible (444) user tokens that have been seen on chain, so that the API serves the
-- metadata of a 333 or 444 subject only when its token exists (ADR 0008). The metadata itself is in cip68_metadata.
CREATE TABLE cip68_user_token (
    unit     VARCHAR(120) PRIMARY KEY, -- NOSONAR: plsql:VarcharUsageCheck, VARCHAR is right on MySQL
    slot     BIGINT       NOT NULL,
    tx_hash  VARCHAR(64)  NOT NULL -- NOSONAR: plsql:VarcharUsageCheck, VARCHAR is right on MySQL
);

CREATE INDEX idx_cip68_user_token_slot ON cip68_user_token(slot);

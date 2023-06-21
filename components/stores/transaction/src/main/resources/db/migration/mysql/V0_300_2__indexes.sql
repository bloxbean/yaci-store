CREATE INDEX idx_transaction_block
    ON transaction(block);

CREATE INDEX idx_transaction_block_hash
    ON transaction(block_hash);

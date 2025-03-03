CREATE INDEX idx_transaction_epoch
    ON transaction(epoch);

CREATE INDEX idx_withdrawal_epoch
    ON withdrawal (epoch);

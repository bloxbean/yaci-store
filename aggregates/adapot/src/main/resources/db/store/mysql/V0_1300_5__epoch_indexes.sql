DROP INDEX IF EXISTS idx_epoch_stake_active_epoch ON epoch_stake;
DROP INDEX IF EXISTS idx_epoch_stake_active_epoch_pool_id ON epoch_stake;
DROP INDEX IF EXISTS epoch_stake_active_epoch_address_index ON epoch_stake;

CREATE INDEX idx_epoch_stake_epoch_pool_id
    ON epoch_stake (epoch, pool_id);

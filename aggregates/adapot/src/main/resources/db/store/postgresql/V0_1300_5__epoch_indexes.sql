DROP INDEX IF EXISTS idx_epoch_stake_active_epoch;
DROP INDEX IF EXISTS idx_epoch_stake_active_epoch_pool_id;
DROP INDEX IF EXISTS epoch_stake_active_epoch_address_index;

CREATE INDEX idx_epoch_stake_epoch_pool_id
    ON epoch_stake (epoch, pool_id);

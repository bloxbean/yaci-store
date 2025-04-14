CREATE INDEX idx_instant_reward_earned_epoch_type
    ON instant_reward (earned_epoch, type);

CREATE INDEX idx_instant_reward_spendable_epoch
    ON instant_reward (spendable_epoch);

CREATE INDEX idx_reward_earned_epoch_type
    ON reward (earned_epoch, type);

CREATE INDEX idx_reward_spendable_epoch
    ON reward (spendable_epoch);

CREATE INDEX idx_epoch_stake_active_epoch
    ON epoch_stake (active_epoch);

CREATE INDEX idx_epoch_stake_active_epoch_pool_id
    ON epoch_stake (active_epoch, pool_id);

CREATE INDEX idx_reward_rest_address
    ON reward_rest (address);

CREATE INDEX idx_reward_rest_earned_epoch
    ON reward_rest (earned_epoch);

CREATE INDEX idx_reward_rest_spendable_epoch
    ON reward_rest (spendable_epoch);


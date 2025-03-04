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




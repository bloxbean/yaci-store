-- Backfill adapot.total_epoch_stake from epoch_stake (PostgreSQL).
--
-- Migration V0_1300_6 adds the column without filling existing rows.
-- This script sets it from the epoch_stake rows that exist when it runs,
-- and only updates rows where total_epoch_stake is NULL.
--
-- Do not run it if epoch-stake pruning is enabled (store.adapot.epoch-stake-pruning-enabled=true):
-- a partially pruned epoch would be stored with a wrong total.

-- adapot[e] holds the total of the stake snapshot taken by the AdaPot job of epoch e (epoch_stake.epoch = e - 1)
UPDATE adapot a
SET total_epoch_stake = s.total_stake
FROM (SELECT epoch, sum(amount) AS total_stake
      FROM epoch_stake
      GROUP BY epoch) s
WHERE a.epoch = s.epoch + 1
  AND a.total_epoch_stake IS NULL;

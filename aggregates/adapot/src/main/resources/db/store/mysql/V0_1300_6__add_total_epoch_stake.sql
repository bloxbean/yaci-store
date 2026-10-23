ALTER TABLE adapot
    ADD COLUMN total_epoch_stake numeric(38);

-- adapot[e] holds the total of the stake snapshot taken by the AdaPot job of epoch e (epoch_stake.epoch = e - 1)
UPDATE adapot a
    JOIN (SELECT epoch, sum(amount) AS total_stake
          FROM epoch_stake
          GROUP BY epoch) s ON a.epoch = s.epoch + 1
SET a.total_epoch_stake = s.total_stake;

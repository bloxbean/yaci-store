ALTER TABLE adapot
    ADD COLUMN total_epoch_stake numeric(38);

-- adapot[e] holds the total of the stake snapshot taken by the AdaPot job of epoch e (epoch_stake.epoch = e - 1)
UPDATE adapot a
SET total_epoch_stake = (SELECT sum(s.amount)
                         FROM epoch_stake s
                         WHERE s.epoch = a.epoch - 1);

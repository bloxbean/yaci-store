-- CREATE VIEW reward_withdrawable_view AS
-- WITH last_withdrawal AS (
--     SELECT address, MAX(slot) AS max_slot
--     FROM withdrawal
--     GROUP BY address
-- ),
--      spendable_rewards AS (
--          SELECT r.address, r.spendable_epoch, SUM(r.amount) AS withdrawable_reward
--          FROM reward r
--                   LEFT JOIN last_withdrawal lw ON r.address = lw.address
--          WHERE (lw.max_slot IS NULL OR r.slot > lw.max_slot)
--          GROUP BY r.address, r.spendable_epoch
--      )
-- SELECT wr.address, wr.spendable_epoch as epoch,  wr.withdrawable_reward as amount
-- FROM spendable_rewards wr;
--
--
-- CREATE VIEW reward_total_view AS
-- WITH last_withdrawal AS (
--     SELECT address, MAX(slot) AS max_slot
--     FROM withdrawal
--     GROUP BY address
-- ),
--      total_rewards AS (
--          SELECT r.address, r.earned_epoch, SUM(r.amount) AS total_reward
--          FROM reward r
--                   LEFT JOIN last_withdrawal lw ON r.address = lw.address
--          WHERE (lw.max_slot IS NULL OR r.slot > lw.max_slot)
--          GROUP BY r.address, r.earned_epoch
--      )
-- SELECT tr.address, tr.earned_epoch as epoch, tr.total_reward as amount
-- FROM total_rewards tr;

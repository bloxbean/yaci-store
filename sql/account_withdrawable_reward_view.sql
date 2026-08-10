-- Reference view for current withdrawable rewards by stake address.
CREATE OR REPLACE VIEW account_withdrawable_reward AS
SELECT
  combined.address,
  COALESCE(SUM(CASE WHEN combined.spendable_epoch <= ce.curr_epoch
                    THEN combined.amount ELSE 0 END), 0) AS amount,
  ce.curr_epoch AS epoch,
  ce.curr_slot AS slot
FROM (
  SELECT address, amount, spendable_epoch, slot FROM reward
  UNION ALL
  SELECT address, amount, spendable_epoch, slot FROM reward_rest
  UNION ALL
  SELECT address, amount, spendable_epoch, slot FROM instant_reward
) combined
LEFT JOIN (
  SELECT address, MAX(slot) AS max_slot
  FROM withdrawal
  GROUP BY address
) lw ON lw.address = combined.address
CROSS JOIN (
  SELECT
    (SELECT MAX(epoch) FROM epoch_param) AS curr_epoch,
    (SELECT MAX(slot)  FROM block)       AS curr_slot
) ce
WHERE combined.slot > COALESCE(lw.max_slot, -1)
GROUP BY combined.address, ce.curr_epoch, ce.curr_slot;

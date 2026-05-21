-- Reference query for current withdrawable rewards for one stake address.
WITH
  ce AS (
    SELECT
      (SELECT MAX(epoch) FROM epoch_param) AS curr_epoch,
      (SELECT MAX(slot)  FROM block)       AS curr_slot
  ),
  lw AS (
    SELECT MAX(slot) AS max_slot
    FROM withdrawal
    WHERE address = :address
  ),
  combined AS (
    SELECT address, amount, spendable_epoch
    FROM reward r
    CROSS JOIN lw
    WHERE r.address = :address
      AND (lw.max_slot IS NULL OR r.slot > lw.max_slot)

    UNION ALL

    SELECT address, amount, spendable_epoch
    FROM reward_rest rr
    CROSS JOIN lw
    WHERE rr.address = :address
      AND (lw.max_slot IS NULL OR rr.slot > lw.max_slot)

    UNION ALL

    SELECT address, amount, spendable_epoch
    FROM instant_reward ir
    CROSS JOIN lw
    WHERE ir.address = :address
      AND (lw.max_slot IS NULL OR ir.slot > lw.max_slot)
  )
SELECT
  :address AS address,
  COALESCE(SUM(CASE WHEN combined.spendable_epoch <= ce.curr_epoch
                    THEN combined.amount ELSE 0 END), 0) AS amount,
  ce.curr_epoch AS epoch,
  ce.curr_slot AS slot
FROM ce
LEFT JOIN combined ON TRUE
GROUP BY ce.curr_epoch, ce.curr_slot;

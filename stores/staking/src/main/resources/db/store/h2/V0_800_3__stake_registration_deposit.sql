ALTER TABLE stake_registration ADD COLUMN deposit BIGINT;

-- Historical versions calculated stake-key registration deposits as 2 ADA. Deregistrations remain
-- NULL so their refunds can be resolved from the active registration lifecycle.
-- Networks with a different historical key deposit require a re-sync for exact values.
UPDATE stake_registration SET deposit = 2000000
WHERE type = 'STAKE_REGISTRATION' AND deposit IS NULL;

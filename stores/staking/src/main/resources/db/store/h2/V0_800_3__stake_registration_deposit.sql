ALTER TABLE stake_registration ADD COLUMN deposit BIGINT;

-- Historical versions calculated all stake-key deposits and refunds as 2 ADA.
-- Networks with a different historical key deposit require a re-sync for exact values.
UPDATE stake_registration SET deposit = 2000000 WHERE deposit IS NULL;

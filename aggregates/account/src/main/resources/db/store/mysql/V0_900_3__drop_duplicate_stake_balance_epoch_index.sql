SET @drop_idx = (SELECT IF(COUNT(*) > 0,
                           'DROP INDEX idx_stake_addr_balance_epoch ON stake_address_balance',
                           'SELECT 1')
                 FROM information_schema.statistics
                 WHERE table_schema = DATABASE()
                   AND table_name = 'stake_address_balance'
                   AND index_name = 'idx_stake_addr_balance_epoch');
PREPARE stmt FROM @drop_idx;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

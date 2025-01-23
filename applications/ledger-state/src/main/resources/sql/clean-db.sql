-- To clean aggregation specific tables and start aggregation

-- set search_path to dev;

delete from cursor_ where id = 1000;

truncate address_balance;
truncate stake_address_balance;
truncate account_config;

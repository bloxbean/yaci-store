-- Table: address_balance_change_tracker
drop table if exists address_balance_change_tracker;
create table address_balance_change_tracker
(
    address       varchar(500) primary key not null
);

-- Table: stake_address_balance_change_tracker

drop table if exists stake_address_balance_change_tracker;
create table stake_address_balance_change_tracker
(
    address varchar(255) primary key not null
);

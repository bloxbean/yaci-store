drop table if exists account_config;
create table account_config
(
    config_id varchar(100),
    status varchar(50),
    slot bigint,
    block  bigint,
    block_hash varchar(64),
    primary key (config_id)
)

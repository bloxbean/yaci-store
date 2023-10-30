drop table if exists account_config;
create table account_config
(
    config_id varchar(100),
    status varchar(50),
    block  bigint,
    primary key (config_id)
)

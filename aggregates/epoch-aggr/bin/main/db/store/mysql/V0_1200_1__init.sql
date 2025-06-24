drop table if exists epoch;
create table epoch
(
    number bigint       not null primary key,
    block_count         int             null,
    transaction_count   bigint          null,
    total_output        numeric(38)     null,
    total_fees          bigint          null,
    start_time          bigint          null,
    end_time            bigint          null,
    max_slot            bigint          null,
    create_datetime     timestamp,
    update_datetime     timestamp
);

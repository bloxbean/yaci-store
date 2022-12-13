create table cursor
(
    id          integer not null
            primary key,
    slot        bigint,
    block_hash  varchar(255),
    block_number bigint,
    create_datetime  timestamp,
    update_datetime  timestamp
);

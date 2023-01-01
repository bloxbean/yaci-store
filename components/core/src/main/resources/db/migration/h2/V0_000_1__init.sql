drop table if exists cursor_;
create table cursor_
(
    id          integer not null,
    block_number bigint,
    slot        bigint,
    block_hash  varchar(255),
    create_datetime  timestamp,
    update_datetime  timestamp,
    primary key (id, block_number)
);

CREATE INDEX idx_cursor_id
    ON cursor_(id);

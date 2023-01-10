drop table if exists cursor_;
create table cursor_
(
    id          integer not null,
    slot        bigint,
    block_number bigint,
    block_hash  varchar(255),
    create_datetime  timestamp,
    update_datetime  timestamp,
    primary key (id, slot)
);

CREATE INDEX idx_cursor_id
    ON cursor_(id);

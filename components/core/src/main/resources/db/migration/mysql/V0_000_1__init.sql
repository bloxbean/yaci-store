drop table if exists cursor_;
create table cursor_
(
    id          int not null,
    slot        bigint,
    block_number bigint,
    block_hash  varchar(255),
    create_datetime  timestamp,
    update_datetime  timestamp,
    primary key (id, slot)
);

CREATE INDEX idx_cursor_id
    ON cursor_(id);
CREATE INDEX idx_cursor_slot
    ON cursor_(slot);
CREATE INDEX idx_cursor_block_number
    ON cursor_(block_number);
CREATE INDEX idx_cursor_block_hash
    ON cursor_(block_hash);

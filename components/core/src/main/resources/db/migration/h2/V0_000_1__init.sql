drop table if exists cursor_;
create table cursor_
(
    id          integer not null,
    block_hash  varchar(255),
    slot        bigint,
    block_number bigint,
    era         int,
    prev_block_hash varchar(255),
    create_datetime  timestamp,
    update_datetime  timestamp,
    primary key (id, block_hash)
);

CREATE INDEX idx_cursor_id
    ON cursor_(id);
CREATE INDEX idx_cursor_slot
    ON cursor_(slot);
CREATE INDEX idx_cursor_block_number
    ON cursor_(block_number);
CREATE INDEX idx_cursor_block_hash
    ON cursor_(block_hash);

create table era
(
    era        int not null primary key,
    start_slot bigint not null,
    block     bigint not null,
    block_hash varchar(255) not null
);

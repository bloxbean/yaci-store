drop table if exists drep_dist;
create table drep_dist
(
    drep_hash       varchar(56),
    drep_id         varchar(255),
    amount          bigint,
    epoch           int,
    update_datetime timestamp,
    primary key (drep_id, epoch)
);

CREATE INDEX idx_drep_dist_drep_id
    ON drep_dist (drep_id);

CREATE INDEX idx_drep_dist_epoch
    ON drep_dist (epoch);

drop table if exists drep;
create table drep
(
    drep_id           varchar(255),
    drep_hash         varchar(56),
    tx_hash           varchar(64) not null,
    cert_index        int         not null,
    tx_index          int         not null,
    cert_type         varchar(40),
    status            varchar(50),
    deposit           bigint,
    epoch             int,
    active_epoch      int,
    inactive_epoch    int,
    retire_epoch      int,
    registration_slot bigint,
    slot              bigint,
    block_hash        varchar(64),
    block             bigint,
    block_time        bigint,
    update_datetime   timestamp,
    primary key (drep_id, tx_hash, cert_index, slot)
);

CREATE INDEX idx_drep_slot
    ON drep (slot);

CREATE INDEX idx_drep_drep_id
    ON drep (drep_id);

CREATE INDEX idx_drep_epoch
    ON drep (epoch);

CREATE INDEX idx_drep_retire_epoch
    ON drep (retire_epoch);

drop table if exists drep_pv9_stale_clear_event_cache;
create table drep_pv9_stale_clear_event_cache
(
    pv9_max_epoch int,
    event_count bigint not null,
    pv9_source_max_slot bigint not null,
    update_datetime timestamp,
    primary key (pv9_max_epoch)
);

drop table if exists drep_pv9_stale_clear_event;
create table drep_pv9_stale_clear_event
(
    pv9_max_epoch int not null,
    address varchar(255) not null,
    old_drep_hash varchar(56) not null,
    old_drep_type varchar(40) not null,
    stale_slot bigint not null,
    stale_tx_index int not null,
    stale_cert_index int not null,
    unreg_epoch int not null,
    unreg_slot bigint not null,
    unreg_tx_index int not null,
    unreg_cert_index int not null,
    update_datetime timestamp
);

CREATE INDEX idx_drep_pv9_clear_event_epoch_address
    ON drep_pv9_stale_clear_event (pv9_max_epoch, address);

CREATE INDEX idx_drep_pv9_clear_event_unreg_order
    ON drep_pv9_stale_clear_event (pv9_max_epoch, unreg_epoch, unreg_slot, unreg_tx_index, unreg_cert_index);

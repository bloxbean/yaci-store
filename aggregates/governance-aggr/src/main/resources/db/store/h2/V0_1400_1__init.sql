drop table if exists drep_dist;
create table drep_dist
(
    drep_hash       varchar(56),
    drep_type       varchar(40),
    drep_id         varchar(255),
    amount          bigint,
    epoch           int,
    update_datetime timestamp,
    primary key (drep_hash, drep_type, epoch)
);

CREATE INDEX idx_drep_dist_drep_hash
    ON drep_dist (drep_hash);

CREATE INDEX idx_drep_dist_epoch
    ON drep_dist (epoch);

drop table if exists gov_action_proposal_status;
create table gov_action_proposal_status
(
    gov_action_tx_hash varchar(64),
    gov_action_index   int,
    type               varchar(50),
    status             varchar(20),
    epoch              int,
    slot               bigint,
    update_datetime    timestamp,
    primary key (gov_action_tx_hash, gov_action_index, epoch)
);

drop table if exists drep_expiry;
create table drep_expiry
(
    drep_id           varchar(255),
    drep_hash         varchar(56),
    active_until      int,
    epoch             int,
    update_datetime   timestamp,
    primary key  (drep_hash, epoch)
);

CREATE INDEX idx_drep_expiry_epoch
    ON drep_expiry (epoch);

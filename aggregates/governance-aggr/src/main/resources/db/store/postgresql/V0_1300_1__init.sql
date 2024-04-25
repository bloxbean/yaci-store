drop table if exists gov_action_proposal_info;
create table gov_action_proposal_info
(
    tx_hash         varchar(64) not null,
    idx             int         not null,
    expiration      int,
    ratified_epoch  int,
    enacted_epoch   int,
    dropped_epoch   int,
    status          varchar(64),
    create_datetime timestamp,
    update_datetime timestamp,
    primary key (tx_hash, idx)
);

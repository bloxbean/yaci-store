drop table if exists committee_state;
create table committee_state
(
    epoch int,
    state varchar(20),
    update_datetime timestamp,
    primary key epoch
);
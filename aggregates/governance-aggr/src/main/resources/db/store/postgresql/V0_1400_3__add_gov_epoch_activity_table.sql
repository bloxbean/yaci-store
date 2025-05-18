drop table if exists gov_epoch_activity;
create table gov_epoch_activity
(
    epoch               int,
    dormant             boolean,
    update_datetime     timestamp,
    primary key (epoch)
);

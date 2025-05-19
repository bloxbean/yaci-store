drop table if exists gov_epoch_activity;
create table gov_epoch_activity
(
    epoch               int,
    dormant             boolean,
    dormant_epoch_count int,
    update_datetime     timestamp,
    primary key (epoch)
);

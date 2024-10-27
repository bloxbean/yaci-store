drop table if exists reward_calc_jobs;
create table reward_calc_jobs
(
    epoch  int primary key ,
    slot   bigint,
    status varchar(30)   not null
);

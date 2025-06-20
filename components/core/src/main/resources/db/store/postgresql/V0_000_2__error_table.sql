drop table if exists error;
create table error
(
    id                  serial,
    block               bigint,
    error_code          varchar(64) not null,
    reason              text not null,
    details             text,
    update_datetime     timestamp,
    primary key (id)
);

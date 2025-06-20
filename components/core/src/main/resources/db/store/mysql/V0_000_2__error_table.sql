drop table if exists error;
create table error
(
    id                  int not null auto_increment,
    block               bigint,
    error_code          varchar(64) not null,
    reason              text not null,
    details             mediumtext,
    update_datetime     timestamp,
    primary key (id)
);

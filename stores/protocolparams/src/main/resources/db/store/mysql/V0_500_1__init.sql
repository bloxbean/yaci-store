drop table if exists protocol_params;
create table protocol_params
(
    id   bigint
        primary key,
    params json,
    create_datetime  timestamp,
    update_datetime  timestamp
);


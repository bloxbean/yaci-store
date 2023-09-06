alter table block
    add column epoch_slot int;
alter table block
    add column op_cert_hot_vkey varchar(64) null;
alter table block
    add column op_cert_seq_number bigint null;
alter table block
    add column op_cert_kes_period bigint null;
alter table block
    add column op_cert_sigma varchar(256) null;

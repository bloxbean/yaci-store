alter table block
    add column epoch_slot int,
    add column op_cert_hot_vkey varchar(64) null,
    add column op_cert_seq_number bigint null,
    add column op_cert_kes_period bigint null,
    add column op_cert_sigma varchar(256) null;

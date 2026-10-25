-- tx_input.addr_hash is written only on PostgreSQL (see the postgresql migration). The column is added here
-- too so the table has the same columns on every database; it stays null.
alter table tx_input add column addr_hash bigint;

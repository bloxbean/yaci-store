-- tx_input.owner_addr is written only on PostgreSQL (see the postgresql migration). The column is added here
-- too so the table has the same columns on every database; it stays null.
alter table tx_input add column owner_addr varchar(500);

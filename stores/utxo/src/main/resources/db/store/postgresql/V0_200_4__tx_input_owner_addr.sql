-- tx_input.owner_addr: the owner_addr of the output the row spends, so the transactions of an address can be read
-- newest first from an index instead of from the address's whole history (/addresses/{address}/transactions).
-- Written only when store.utxo.tx-input-owner-addr-enabled=true (PostgreSQL 16+), from the first block: see
-- TxInputOwnerAddr, which also fills the rows of a store synced before and builds the indexes at the tip.
-- Adding a nullable column without a default changes only the catalog: no table rewrite.
alter table tx_input add column owner_addr varchar(500);

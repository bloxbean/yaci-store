-- tx_input.addr_hash: hashtextextended(owner_addr, 0) of the output the row spends, so the transactions of an
-- address can be read newest first from an index instead of from the address's whole history
-- (/addresses/{address}/transactions). Written only when store.utxo.tx-input-addr-hash-enabled=true: null
-- during the initial sync, then filled at the tip by TxInputAddrHash, which also builds the indexes.
-- Adding a nullable column without a default changes only the catalog: no table rewrite.
alter table tx_input add column addr_hash bigint;

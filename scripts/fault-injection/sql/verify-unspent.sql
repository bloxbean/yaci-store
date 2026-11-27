-- Unspent table vs the truth derived from address_utxo and tx_input, in one snapshot, up to the table's own position.
begin isolation level repeatable read;
set local work_mem='1GB';
with truth as (
  select a.tx_hash, a.output_index from address_utxo a
  left join tx_input i on i.tx_hash=a.tx_hash and i.output_index=a.output_index
  where i.tx_hash is null
)
select 'missing_from_table' k, count(*) from truth t
  left join address_utxo_unspent u on u.tx_hash=t.tx_hash and u.output_index=t.output_index where u.tx_hash is null
union all
select 'extra_in_table', count(*) from address_utxo_unspent u
  left join truth t on u.tx_hash=t.tx_hash and u.output_index=t.output_index where t.tx_hash is null
union all
select 'table_rows', count(*) from address_utxo_unspent;
select state_id, status, slot, block from store_state;
commit;

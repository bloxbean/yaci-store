-- Rows in address_utxo_unspent that are not unspent according to address_utxo and tx_input, with what spent them.

begin isolation level repeatable read;
set local work_mem='1GB';
create temp table x as
select u.tx_hash, u.output_index, u.slot from address_utxo_unspent u
where not exists (select 1 from address_utxo a where a.tx_hash=u.tx_hash and a.output_index=u.output_index
                  and not exists (select 1 from tx_input i where i.tx_hash=a.tx_hash and i.output_index=a.output_index));
select x.*, (select count(*) from address_utxo a where a.tx_hash=x.tx_hash and a.output_index=x.output_index) in_addr_utxo,
       i.spent_at_slot, i.spent_at_block, left(i.spent_tx_hash,16) spent_by
from x left join tx_input i on i.tx_hash=x.tx_hash and i.output_index=x.output_index order by x.slot;
select slot, block_number from cursor_ order by slot desc limit 1;
commit;

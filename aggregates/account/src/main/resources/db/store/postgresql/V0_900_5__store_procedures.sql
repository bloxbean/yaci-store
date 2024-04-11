drop table if exists address_balance_snapshot;
create table address_balance_snapshot
(
    address         character varying(500) not null,
    unit            character varying(255) not null,
    slot            bigint                 not null,
    quantity        numeric(38, 0),
    addr_full       text,
    block           bigint,
    block_time      bigint,
    epoch           integer,
    update_datetime timestamp without time zone,
    primary key (address, unit, slot)
);

-- create index idx_address_balance_snapshot_address on address_balance_snapshot using btree (address);
-- create index idx_address_balance_snapshot_block on address_balance_snapshot using btree (block);
-- create index idx_address_balance_snapshot_block_time on address_balance_snapshot using btree (block_time);
-- create index idx_address_balance_snapshot_epoch on address_balance_snapshot using btree (epoch);
-- create index idx_address_balance_snapshot_slot on address_balance_snapshot using btree (slot);
-- create index idx_address_balance_snapshot_unit on address_balance_snapshot using btree (unit);


drop table if exists take_snapshot_time_log;
create table take_snapshot_time_log
(
    from_id    numeric,
    to_id      numeric,
    time_taken numeric,
);

drop procedure if exists calculate_address_balance;
CREATE OR REPLACE PROCEDURE
    calculate_address_balance(_from numeric, _to numeric, lastSnapshotSlot NUMERIC)
    LANGUAGE plpgsql
AS
$$
BEGIN
insert into address_balance_snapshot (address,
                                      unit,
                                      quantity,
                                      slot,
                                      block,
                                      block_time,
                                      epoch,
                                      update_datetime)
select address_tx_amount.address         as address,
       address_tx_amount.unit            as unit,
       cast(coalesce(
               sum(address_tx_amount.quantity),
               0
            ) as decimal(38))            as quantity,
       max(address_tx_amount.slot)       as slot,
       max(address_tx_amount.block)      as block,
       max(address_tx_amount.block_time) as block_time,
       max(address_tx_amount.epoch)      as epoch,
       current_timestamp
from address_tx_amount
where (
          address_tx_amount.slot <= lastSnapshotSlot
              and address_tx_amount.address in (select address.address
                                                from address
                                                where address.id between _from and (_to - 1)
                                                    fetch next (_to - _from + 1) rows only)
              )
group by address_tx_amount.address, address_tx_amount.unit
on conflict (address, unit, slot)
    do update
           set quantity   = excluded.quantity,
           slot       = excluded.slot,
           block      = excluded.block,
           block_time = excluded.block_time,
           epoch      = excluded.epoch;
commit;
END;
$$;

drop procedure if exists take_address_balance_snapshot;
CREATE OR REPLACE PROCEDURE
    take_address_balance_snapshot(_from numeric, _to numeric, _batch_size numeric, _lastSnapshotSlot numeric)
    LANGUAGE plpgsql
AS
$$
DECLARE
current_from      numeric;
    current_to        numeric;
    start_insert_time timestamp;
    time_taken        numeric;
BEGIN
    current_from := _from;
    WHILE (current_from + _batch_size) <= _to
        LOOP
            current_to := current_from + _batch_size;
            start_insert_time := clock_timestamp();
            call calculate_address_balance(current_from, current_to, _lastSnapshotSlot);
            time_taken := EXTRACT(EPOCH FROM clock_timestamp() - start_insert_time);
            insert into take_snapshot_time_log(from_id, to_id, time_taken)
            values (current_from, current_to, time_taken);
            commit;
            current_from := current_to;
        END LOOP;
END;
$$;
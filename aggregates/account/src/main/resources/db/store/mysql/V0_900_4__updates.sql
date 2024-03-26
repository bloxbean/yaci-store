-- drop views
drop view if exists address_balance_view;
drop view if exists stake_address_balance_view;

-- drop indexes
drop index idx_address_balance_policy on address_balance;
drop index idx_address_balance_policy_asset on address_balance;

-- drop columns in address_balance
alter table address_balance
drop column policy,
drop column asset_name,
drop column block_hash;

-- drop columns in stake_address_balance
alter table stake_address_balance
drop column stake_credential,
drop column block_hash;

-- add stake_credential column in address table
alter table address add column stake_credential varchar(56);

-- recreate views
create view address_balance_view as
select ab.*
from address_balance ab
         inner join (select address, unit, max(slot) as max_slot
                     from address_balance ab2
                     group by address, unit) max_ab
                    on ab.address = max_ab.address and ab.unit = max_ab.unit and ab.slot = max_ab.max_slot;

create view stake_address_balance_view AS
select sb.*
from stake_address_balance sb
         inner join (select address, MAX(slot) as max_slot
                     from stake_address_balance sb2
                     group by address) max_sb on sb.address = max_sb.address and sb.slot = max_sb.max_slot;
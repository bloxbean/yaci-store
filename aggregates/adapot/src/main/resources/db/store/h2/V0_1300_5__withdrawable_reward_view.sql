drop view if exists stake_address_withdrawable_reward_view;
create force view stake_address_withdrawable_reward_view as
select addresses.address,
       greatest(coalesce(rewards.reward_amount, 0) - coalesce(withdrawals.withdrawal_amount, 0), 0) as withdrawable_amount
from (
    select address from reward
    union
    select address from reward_rest
    union
    select address from instant_reward
    union
    select address from withdrawal
) addresses
left join (
    select all_rewards.address, sum(all_rewards.amount) as reward_amount
    from (
        select address, amount
        from reward
        where spendable_epoch <= (select max(epoch) from epoch_param)
        union all
        select address, amount
        from reward_rest
        where spendable_epoch <= (select max(epoch) from epoch_param)
        union all
        select address, amount
        from instant_reward
        where spendable_epoch <= (select max(epoch) from epoch_param)
    ) all_rewards
    group by all_rewards.address
) rewards on addresses.address = rewards.address
left join (
    select address, sum(amount) as withdrawal_amount
    from withdrawal
    group by address
) withdrawals on addresses.address = withdrawals.address;

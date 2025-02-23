package com.bloxbean.cardano.yaci.store.governanceaggr.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class DRepDistService {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public void takeStakeSnapshot(int epoch) {
        log.info("Taking dRep stake snapshot for epoch : " + epoch);
        // Delete existing snapshot data if any for the epoch using jdbc template
        jdbcTemplate.update("delete from drep_dist where epoch = :epoch", new MapSqlParameterSource().addValue("epoch", epoch));

        String query = """
                  INSERT INTO drep_dist 
                  with ranked_delegations as (
                    select
                      address,
                      drep_id,
                      drep_hash,
                      drep_type,
                      epoch,
                      slot,
                      tx_index,
                      cert_index,
                      row_number() over (
                        partition by address
                        order by
                          slot desc,
                          tx_index desc,
                          cert_index desc
                      ) as rn
                    from
                      delegation_vote
                    where
                      epoch <= :epoch
                  ),
                  drep_status as (
                    select
                      drep_id,
                      drep_hash,
                      status,
                      slot,
                      row_number() over (
                        partition by drep_id
                        order by
                          slot desc,
                          tx_index desc,
                          cert_index desc
                      ) as rn
                    from
                      drep
                    where
                      epoch <= :epoch
                  ),
                  max_slot_balances as (
                    select
                      address,
                      MAX(slot) as max_slot
                    from
                      stake_address_balance
                    where
                      epoch <= :epoch
                    group by
                      address
                  ),
                  active_proposal_deposits  as (
                    select
                      g.return_address,
                      SUM(g.deposit) as deposit
                    from
                      gov_action_proposal g
                      inner join gov_action_proposal_status s on g.tx_hash = s.gov_action_tx_hash
                      and g.idx = s.gov_action_index
                    where
                      s.status = 'ACTIVE'
                      and g.epoch <= :epoch
                      and s.epoch = :epoch
                    group by
                      g.return_address
                  ),
                  last_withdrawal as (
                    select
                      address,
                      MAX(slot) as max_slot
                    from
                      withdrawal
                    where
                      epoch <= :epoch
                    group by
                      address
                  ),
                  pool_refund_rewards as (
                    select
                      r.address,
                      SUM(r.amount) as pool_refund_withdrawable_reward
                    from
                      reward r
                      left join last_withdrawal lw on r.address = lw.address
                    where
                      (
                        lw.max_slot is null
                        or r.slot > lw.max_slot
                      )
                      and r.spendable_epoch <= :epoch
                      and r.type = 'refund'
                    group by
                      r.address
                  ),
                  pool_rewards as (
                    select
                      r.address,
                      SUM(r.amount) as withdrawable_reward
                    from
                      reward r
                      left join last_withdrawal lw on r.address = lw.address
                    where
                      (
                        lw.max_slot is null
                        or r.slot > lw.max_slot
                      )
                      and r.earned_epoch <= :epoch
                      and (
                        r.type = 'member'
                        or r.type = 'leader'
                      )
                      and r.spendable_epoch <= :snapshot_epoch
                    group by
                      r.address
                  ),
                  insta_spendable_rewards as (
                    select
                      r.address,
                      SUM(r.amount) as insta_withdrawable_reward
                    from
                      instant_reward r
                      left join last_withdrawal lw on r.address = lw.address
                    where
                      (
                        lw.max_slot is null
                        or r.slot > lw.max_slot
                      )
                      and r.spendable_epoch <= :snapshot_epoch
                    group by
                      r.address
                  ),
                  spendable_reward_rest as (
                    select
                       r.address,
                       SUM(r.amount) as withdrawable_reward_rest
                    from
                    reward_rest r
                    left join last_withdrawal lw on
                       r.address = lw.address
                    where
                       (
                          lw.max_slot is null
                          or r.slot > lw.max_slot
                        )
                        and r.spendable_epoch <= :epoch
                    group by
                      r.address
                  )               
                  select
                    rd.drep_hash,
                    rd.drep_id,
                    sum(
                      COALESCE(sab.quantity, 0) + COALESCE(r.withdrawable_reward, 0) + COALESCE(
                        pr.pool_refund_withdrawable_reward,
                        0
                      ) + COALESCE(ir.insta_withdrawable_reward, 0) + coalesce(apd.deposit, 0)
                        + COALESCE(rr.withdrawable_reward_rest, 0)
                    ),
                    :epoch,
                    NOW()
                  from
                    ranked_delegations rd
                    left join drep_status ds on rd.drep_id = ds.drep_id
                    and rd.rn = 1
                    left join pool_rewards r on rd.address = r.address
                    left join pool_refund_rewards pr on rd.address = pr.address
                    left join insta_spendable_rewards ir on rd.address = ir.address
                    left join active_proposal_deposits  apd on apd.return_address = rd.address
                    left join max_slot_balances msb on msb.address = rd.address
                    left join stake_address_balance sab on msb.address = sab.address and msb.max_slot = sab.slot
                    left join spendable_reward_rest rr ON rd.address = rr.address
                  where
                    ds.status = 'ACTIVE'
                    and ds.rn = 1
                    and not exists (
                      select
                        1
                      from
                        stake_registration sd
                      where
                        sd.address = rd.address
                        and sd.type = 'STAKE_DEREGISTRATION'
                        and sd.epoch <= :epoch
                        and (
                          sd.slot > rd.slot
                          or (
                            sd.slot = rd.slot
                            and sd.tx_index > rd.tx_index
                          )
                          or (
                            sd.slot = rd.slot
                            and sd.tx_index = rd.tx_index
                            and sd.cert_index > rd.cert_index
                          )
                        )
                    )
                  group by
                    rd.drep_hash,
                    rd.drep_id
                  union all
                  select
                    rd.drep_type,
                    null,
                    sum(
                      COALESCE(sab.quantity, 0) + COALESCE(r.withdrawable_reward, 0) + COALESCE(
                        pr.pool_refund_withdrawable_reward,
                        0
                      ) + COALESCE(ir.insta_withdrawable_reward, 0) + coalesce(apd.deposit, 0)
                    ),
                    :epoch,
                    NOW()
                  from
                    ranked_delegations rd                      
                    left join pool_rewards r on rd.address = r.address
                    left join pool_refund_rewards pr on rd.address = pr.address
                    left join insta_spendable_rewards ir on rd.address = ir.address
                    left join active_proposal_deposits  apd on apd.return_address = rd.address
                    left join max_slot_balances msb on msb.address = rd.address
                    left join stake_address_balance sab on msb.address = sab.address and msb.max_slot = sab.slot
                    left join spendable_reward_rest rr ON rd.address = rr.address                                               
                  where
                    rd.rn=1 AND (rd.drep_type = 'ABSTAIN' OR rd.drep_type = 'NO_CONFIDENCE') 
                    AND
                    not exists (
                      select
                        1
                      from
                        stake_registration sd
                      where
                        sd.address = rd.address
                        and sd.type = 'STAKE_DEREGISTRATION'
                        and sd.epoch <= :epoch
                        and (
                          sd.slot > rd.slot
                          or (
                            sd.slot = rd.slot
                            and sd.tx_index > rd.tx_index
                          )
                          or (
                            sd.slot = rd.slot
                            and sd.tx_index = rd.tx_index
                            and sd.cert_index > rd.cert_index
                          )
                        )
                    )
                  group by
                    rd.drep_type
                  
                """;

        var params = new MapSqlParameterSource();
        params.addValue("epoch", epoch);
        params.addValue("snapshot_epoch", epoch + 1);

        jdbcTemplate.update(query, params);

        log.info("DRep Stake Distribution snapshot for epoch : {} is taken", epoch);
        log.info(">>>>>>>>>>>>>>>>>>>> DRep Stake Distribution Stake Snapshot taken for epoch : {} <<<<<<<<<<<<<<<<<<<<", epoch);

    }
}

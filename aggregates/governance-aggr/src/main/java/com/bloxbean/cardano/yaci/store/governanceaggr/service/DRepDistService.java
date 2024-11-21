package com.bloxbean.cardano.yaci.store.governanceaggr.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class DRepDistService {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void takeStakeSnapshot(int epoch) {
        log.info("Taking dRep stake snapshot for epoch : " + epoch);

        String query = """ 
                   WITH RankedDelegations AS (
                       SELECT 
                           address, 
                           drep_id,
                           drep_hash,
                           drep_type,
                           epoch,
                           slot,
                           tx_index,
                           cert_index,
                           ROW_NUMBER() OVER (
                               PARTITION BY address
                               ORDER BY slot DESC, tx_index DESC, cert_index DESC
                           ) AS rn
                       FROM
                           delegation_vote
                       WHERE
                           epoch <= :epoch
                   ),
                   
                   DRepStatus AS (
                   	 SELECT
                   	     drep_id,
                   	     drep_hash,
                   	     status,
                   	     slot,
                   	     ROW_NUMBER() OVER (
                   	         PARTITION BY drep_id
                   	         ORDER BY slot DESC, tx_index DESC, cert_index DESC
                   	     ) AS rn
                   	 FROM
                   	     drep
                   	 WHERE epoch <= :epoch
                   ),
                                   
                   MaxSlotBalances AS (
                       SELECT
                           address,
                           MAX(slot) AS max_slot
                       FROM
                           stake_address_balance_1
                       WHERE
                           epoch <= :epoch
                       GROUP BY
                           address
                   ),
                   
                   ActiveProposalDeposits AS (
                       SELECT
                           g.return_address,
                           SUM(g.deposit) AS active_deposit
                       FROM
                           gov_action_proposal g
                       INNER JOIN gov_action_proposal_status s ON
                           g.tx_hash = s.gov_action_tx_hash
                           AND g.idx = s.gov_action_index
                       WHERE
                           s.status = 'ACTIVE'
                           AND g.epoch <= :epoch
                       GROUP BY
                           g.return_address
                   ),
                   
                   AddressEpochStake AS (
                       SELECT
                          es.address,
                           sum(es.amount) AS amount
                          from epoch_stake_1 es 
                       where es.epoch = :epoch
                      
                       GROUP BY
                           es.address
                   )
                   
                   INSERT INTO drep_dist
                   SELECT
                       ds.drep_hash as drep_hash,
                       ds.drep_id as drep_id,
                        SUM(
                           coalesce(aa.amount, 0)
                           + CASE
                               WHEN aa.amount IS NOT NULL THEN 0
                               ELSE coalesce(sab.quantity,0)
                             END
                            
                           + coalesce(apd.active_deposit, 0)
                       ) as amount,
                       :epoch,
                       NOW()
                   FROM
                       DRepStatus ds
                   LEFT JOIN RankedDelegations rd ON
                       rd.drep_id = ds.drep_id AND rd.rn = 1
                   LEFT JOIN AddressEpochStake aa ON
                       rd.address = aa.address
                   left join ActiveProposalDeposits apd on
                   	apd.return_address = rd.address
                   left join MaxSlotBalances msb on
                   	msb.address = rd.address
                   left join stake_address_balance sab
                   	on msb.address = sab.address
                   	and msb.max_slot = sab.slot
                       and NOT EXISTS (
                                           SELECT 1
                                           FROM stake_registration sd
                                           WHERE sd.address = rd.address
                                               AND sd.type = 'STAKE_DEREGISTRATION'
                                               AND sd.epoch <= :epoch
                                               AND (
                                                   sd.slot > rd.slot
                                                   OR (sd.slot = rd.slot AND sd.tx_index > rd.tx_index)
                                                   OR (sd.slot = rd.slot AND sd.tx_index = rd.tx_index
                                                   AND sd.cert_index > rd.cert_index)
                                               )
                                       )
                      
                   WHERE
                       ds.status = 'ACTIVE' and ds.rn = 1
                   GROUP BY
                       ds.drep_hash,
                       ds.drep_id
                      
                      
                   UNION ALL
                   SELECT
                       NULL AS drep_hash,
                       drep_type_info.drep_type AS drep_id,
                         SUM(
                           coalesce(aa.amount, 0)
                           + CASE
                               WHEN aa.amount IS NOT NULL THEN 0
                               ELSE coalesce(sab.quantity, 0)
                             END
                           + coalesce(apd.active_deposit, 0)
                       ) as amount,
                       :epoch,
                       NOW()
                   FROM
                       (VALUES ('ABSTAIN'), ('NO_CONFIDENCE')) AS drep_type_info(drep_type)
                   LEFT JOIN RankedDelegations rd ON
                       rd.drep_type = drep_type_info.drep_type AND rd.rn = 1
                   LEFT JOIN AddressEpochStake aa ON
                       rd.address = aa.address
                   left join ActiveProposalDeposits apd on
                   	apd.return_address = rd.address
                   left join MaxSlotBalances msb on
                   	msb.address = rd.address
                   left join stake_address_balance sab
                   	on msb.address = sab.address
                   	and msb.max_slot = sab.slot
                       and NOT EXISTS (
                                           SELECT 1
                                           FROM stake_registration sd
                                           WHERE sd.address = rd.address
                                               AND sd.type = 'STAKE_DEREGISTRATION'
                                               AND sd.epoch <= :epoch
                                               AND (
                                                   sd.slot > rd.slot
                                                   OR (sd.slot = rd.slot AND sd.tx_index > rd.tx_index)
                                                   OR (sd.slot = rd.slot AND sd.tx_index = rd.tx_index
                                                   AND sd.cert_index > rd.cert_index)
                                               )
                                       )
                   GROUP BY
                       drep_type_info.drep_type
                """;

        var params = new MapSqlParameterSource();
        params.addValue("epoch", epoch);

        jdbcTemplate.update(query, params);

        log.info("Stake snapshot for epoch : {} is taken", epoch);
        log.info(">>>>>>>>>>>>>>>>>>>> Stake Snapshot taken for epoch : {} <<<<<<<<<<<<<<<<<<<<", epoch);

    }
}

package com.bloxbean.cardano.yaci.store.governanceaggr.service;

import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionStatus;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import com.bloxbean.cardano.yaci.store.epoch.domain.EpochParam;
import com.bloxbean.cardano.yaci.store.epoch.processor.EraGenesisProtocolParamsUtil;
import com.bloxbean.cardano.yaci.store.epoch.storage.EpochParamStorage;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.GovActionProposalStatus;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.GovActionProposalStatusStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class DRepDistService {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final EraService eraService;
    private final EpochParamStorage epochParamStorage;
    private final GovActionProposalStatusStorage govActionProposalStatusStorage;
    private final StoreProperties storeProperties;
    private final EraGenesisProtocolParamsUtil eraGenesisProtocolParamsUtil;

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public void takeStakeSnapshot(int epoch) {
        if (eraService.getEraForEpoch(epoch).getValue() < Era.Conway.getValue()) {
            return;
        }

        boolean isInBootstrapPhase = true;
        int maxBootstrapPhaseEpoch = 0;

        if (isPublicNetwork()) {
            Optional<EpochParam> epochParamOpt = epochParamStorage.getProtocolParams(epoch);

            if (epochParamOpt.isPresent()) {
                var protocolParams = epochParamOpt.get().getParams();
                if (protocolParams.getProtocolMajorVer() >= 10) {
                    isInBootstrapPhase = false;
                    // find max epoch of the bootstrap phase
                    maxBootstrapPhaseEpoch = govActionProposalStatusStorage.findByTypeAndStatusAndEpochLessThan(GovActionType.HARD_FORK_INITIATION_ACTION, GovActionStatus.RATIFIED, epoch)
                            .stream()
                            .sorted(Comparator.comparingInt(GovActionProposalStatus::getEpoch))
                            .toList()
                            .getFirst()
                            .getEpoch();
                }
            }
        } else {
            ProtocolParams genesisProtocolParams = eraGenesisProtocolParamsUtil
                    .getGenesisProtocolParameters(Era.Conway, null, storeProperties.getProtocolMagic())
                    .orElse(null);

            if (genesisProtocolParams != null && genesisProtocolParams.getProtocolMajorVer() >= 10) {
                isInBootstrapPhase = false;
            }
        }

        log.info("Taking dRep stake snapshot for epoch : " + epoch);
        // Delete existing snapshot data if any for the epoch using jdbc template
        jdbcTemplate.update("delete from drep_dist where epoch = :snapshot_epoch", new MapSqlParameterSource().addValue("snapshot_epoch", epoch + 1));

        //Drop temp tables in parallel
        List<String> dropQueries = List.of(
            "DROP TABLE IF EXISTS ss_drep_ranked_delegations",
            "DROP TABLE IF EXISTS ss_drep_status",
            "DROP TABLE IF EXISTS ss_gov_active_proposal_deposits",
            "DROP TABLE IF EXISTS ss_gov_spendable_reward_rest",
            "DROP TABLE IF EXISTS ss_gov_pool_refund_rewards"
        );

        for (String query : dropQueries) {
            jdbcTemplate.update(query, Map.of());
        }
        log.info("Dropped existing temp tables for DRep distribution !!!");

        String rankedDelegationsQuery = """
                CREATE TABLE ss_drep_ranked_delegations AS
                    WITH ranked AS (
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
                    )
                    SELECT
                        address,
                        drep_id,
                        drep_hash,
                        drep_type,
                        epoch,
                        slot,
                        tx_index,
                        cert_index
                    FROM ranked
                    WHERE rn = 1
                """;
        String drepStatusQuery = """
                CREATE TABLE ss_drep_status AS
                WITH last_reg AS (
                    SELECT
                        dr.drep_id,
                        dr.drep_hash,
                        dr.cred_type,
                        dr.epoch AS registration_epoch,
                        dr.slot  AS registration_slot,
                        dr.tx_index  AS registration_tx_index,
                        dr.cert_index AS registration_cert_index
                    FROM (
                        SELECT
                            drep_id,
                            drep_hash,
                            cred_type,
                            epoch,
                            slot,
                            tx_index,
                            cert_index,
                            ROW_NUMBER() OVER (
                                PARTITION BY drep_hash
                                ORDER BY slot DESC, tx_index DESC, cert_index DESC
                            ) AS rn
                        FROM drep_registration
                        WHERE epoch <= :epoch
                          AND type = 'REG_DREP_CERT'
                    ) dr
                    WHERE dr.rn = 1
                ),
                last_unreg AS (
                    SELECT
                        du.drep_id,
                        du.drep_hash,
                        du.epoch AS unregistration_epoch,
                        du.slot  AS unregistration_slot,
                        du.tx_index  AS unregistration_tx_index,
                        du.cert_index AS unregistration_cert_index
                    FROM (
                        SELECT
                            drep_id,
                            drep_hash,
                            epoch,
                            slot,
                            tx_index,
                            cert_index,
                            ROW_NUMBER() OVER (
                                PARTITION BY drep_hash
                                ORDER BY slot DESC, tx_index DESC, cert_index DESC
                            ) AS rn
                        FROM drep_registration
                        WHERE epoch <= :epoch
                          AND type = 'UNREG_DREP_CERT'
                    ) du
                    WHERE du.rn = 1
                )
                SELECT
                    d.drep_id,
                    d.drep_hash,
                    d.tx_index,
                    d.cert_index,
                    d.type,
                    d.slot,
                    lr.cred_type,
                    lr.registration_epoch,
                    lr.registration_slot,
                    lr.registration_tx_index,
                    lr.registration_cert_index,
                    
                    lu.unregistration_slot,
                    lu.unregistration_tx_index,
                    lu.unregistration_cert_index,
                
                    ROW_NUMBER() OVER (
                        PARTITION BY d.drep_hash
                        ORDER BY d.slot DESC, d.tx_index DESC, d.cert_index DESC
                    ) AS rn
                FROM drep_registration d
                LEFT JOIN last_reg lr
                       ON d.drep_hash = lr.drep_hash
                LEFT JOIN last_unreg lu 
                        ON d.drep_hash = lu.drep_hash
                WHERE d.epoch <= :epoch
                """;

        String activeProposalDepositsQuery = """
                CREATE TABLE ss_gov_active_proposal_deposits AS
                select
                      g.return_address,
                      SUM(g.deposit) as deposit
                    from
                      gov_action_proposal g
                      left join gov_action_proposal_status s on g.tx_hash = s.gov_action_tx_hash
                      and g.idx = s.gov_action_index
                    where
                     (s.status = 'ACTIVE'
                     		and g.epoch < :epoch
                     		and s.epoch = :epoch)
                     	or (s.status is null
                     		and g.epoch = :epoch)
                    group by
                      g.return_address
                """;

        String spendableRewardRestQuery = """
                CREATE TABLE ss_gov_spendable_reward_rest AS
                select
                    r.address,
                    SUM(r.amount) as withdrawable_reward_rest,
                    :epoch as mark_epoch
                from
                    reward_rest r
                left join ss_last_withdrawal lw on
                    r.address = lw.address
                where
                    (lw.max_slot is null
                        or r.slot > lw.max_slot)
                    and r.spendable_epoch <= :snapshot_epoch
                group by
                    r.address
                """;

        String poolRefundRewardsQuery = """
                CREATE TABLE ss_gov_pool_refund_rewards AS
                SELECT r.address, SUM(r.amount) AS pool_refund_withdrawable_reward, :epoch as mark_epoch
                FROM reward r
                         LEFT JOIN ss_last_withdrawal lw ON r.address = lw.address
                WHERE (lw.max_slot IS NULL OR r.slot > lw.max_slot)
                  AND r.spendable_epoch <= :snapshot_epoch and r.type = 'refund'
                GROUP BY r.address
                """;

        List<String> createTableQueries = List.of(
                rankedDelegationsQuery,
                drepStatusQuery,
                activeProposalDepositsQuery,
                spendableRewardRestQuery,
                poolRefundRewardsQuery
        );

        long start = System.currentTimeMillis();
        var epochParam = new MapSqlParameterSource();
        epochParam.addValue("epoch", epoch);
        epochParam.addValue("snapshot_epoch", epoch);

        for (String query : createTableQueries) {
            log.info("Executing query : " + query);
            jdbcTemplate.update(query, epochParam);
            log.info(">> Temp table created for Drep dist query : " + query);
        }
        long end = System.currentTimeMillis();
        log.info(">> Created all temp tables for Drep dist << " + (end - start) + " ms");

        // Create indexes in parallel (if required, otherwise skip this part)
        List<String> createIndexQueries = List.of(
                "CREATE INDEX idx_ss_drep_ranked_delegations_address ON ss_drep_ranked_delegations(address)",
                "CREATE INDEX idx_ss_drep_ranked_delegations_drep_hash ON ss_drep_ranked_delegations(drep_hash)",
                "CREATE INDEX idx_ss_drep_ranked_delegations_rn ON ss_ranked_delegations(rn)",
                "CREATE INDEX idx_ss_gov_spendable_reward_rest_address ON ss_gov_spendable_reward_rest(address)",
                "CREATE INDEX idx_ss_gov_pool_refund_rewards_address ON ss_gov_pool_refund_rewards(address)",
                "CREATE INDEX idx_ss_drep_status_drep_hash ON ss_drep_status(drep_hash)",
                "CREATE INDEX idx_ss_drep_status_drep_type ON ss_drep_status(type)",
                "CREATE INDEX idx_ss_gov_active_proposal_deposits_ret_address ON ss_gov_active_proposal_deposits(return_address)"
        );

        start = System.currentTimeMillis();
        for (String indexQuery : createIndexQueries) {
            jdbcTemplate.update(indexQuery, Map.of());
        }
        end = System.currentTimeMillis();
        log.info(">> Indexes created for DRep dist temp tables <<" + (end - start) + " ms");

        String excludeDelegationCondition;

        if (!isInBootstrapPhase) {

           /*
            After the bootstrap phase, while calculating DRep voting power for each DRep,
            for delegations created in the bootstrap phase:
            we need to exclude those delegations that were created after the DRep registration,
            except for delegations and registrations made in the same transaction.
            */

            excludeDelegationCondition = """
                and exists (
                    select 1 from ss_drep_status ds
                    where ds.drep_hash = rd.drep_hash
                    and ds.cred_type = rd.drep_type
                    and ds.rn = 1
                    and (ds.type = 'REG_DREP_CERT' or ds.type = 'UPDATE_DREP_CERT')
                    and ( 
                        rd.slot > ds.registration_slot
                        or (rd.slot = ds.registration_slot and rd.tx_index > ds.registration_tx_index)
                        or (rd.slot = ds.registration_slot and rd.tx_index <= ds.registration_tx_index and rd.epoch <= :max_bootstrap_phase_epoch)
                        or (
                            rd.slot < ds.registration_slot 
                            and (
                                ds.unregistration_slot is null
                                or
                                ( rd.slot > ds.unregistration_slot 
                                    or (rd.slot = ds.unregistration_slot and rd.tx_index > ds.unregistration_tx_index)
                                    or (rd.slot = ds.unregistration_slot and rd.tx_index = ds.unregistration_tx_index and rd.cert_index > ds.unregistration_cert_index)
                                )
                            )
                            and rd.epoch <= :max_bootstrap_phase_epoch and ds.registration_epoch <= :max_bootstrap_phase_epoch
                        )
                        or (rd.slot = ds.registration_slot and rd.tx_index = ds.registration_tx_index and rd.epoch > :max_bootstrap_phase_epoch and rd.cert_index > ds.registration_cert_index)
                    )
                )
            """;

        } else {
            excludeDelegationCondition = """
                and exists (
                    select 1 from ss_drep_status ds
                    where ds.drep_hash = rd.drep_hash
                    and ds.cred_type = rd.drep_type
                    and ds.rn = 1 
                    and (ds.type = 'REG_DREP_CERT' or ds.type = 'UPDATE_DREP_CERT')
                )
            """;
        }

        String query1 = """
                  INSERT INTO drep_dist               
                  select
                    rd.drep_hash,
                    rd.drep_type,
                    rd.drep_id,
                    sum(
                      COALESCE(sab.quantity, 0) 
                          + COALESCE(r.withdrawable_reward, 0) 
                          + COALESCE(pr.pool_refund_withdrawable_reward, 0) 
                          + coalesce(apd.deposit, 0) 
                          + COALESCE(rr.withdrawable_reward_rest, 0)
                    ),
                    :snapshot_epoch,
                    NOW()
                  from
                    ss_drep_ranked_delegations rd
                    left join ss_pool_rewards r on rd.address = r.address
                    left join ss_gov_pool_refund_rewards pr on rd.address = pr.address
                    left join ss_gov_active_proposal_deposits  apd on apd.return_address = rd.address
                    left join ss_max_slot_balances msb on msb.address = rd.address
                    left join stake_address_balance sab on msb.address = sab.address and msb.max_slot = sab.slot
                    left join ss_gov_spendable_reward_rest rr ON rd.address = rr.address
                    left join stake_registration sd
                                          ON sd.address = rd.address
                                              AND sd.type = 'STAKE_DEREGISTRATION'
                                              AND sd.epoch <= :epoch
                                              AND (
                                                 sd.slot > rd.slot OR
                                                 (sd.slot = rd.slot AND sd.tx_index > rd.tx_index) OR
                                                 (sd.slot = rd.slot AND sd.tx_index = rd.tx_index AND sd.cert_index > rd.cert_index)
                                                 )
                  where
                    sd.address IS NULL
                    """ + excludeDelegationCondition + """
                  group by
                    rd.drep_hash,
                    rd.drep_type,
                    rd.drep_id
                    """;

        String query2 = """
                  INSERT INTO drep_dist
                  select
                    '00000000000000000000000000000000000000000000000000000000',
                    rd.drep_type,
                    null,
                    sum(
                      COALESCE(sab.quantity, 0)
                          + COALESCE(r.withdrawable_reward, 0) 
                          + COALESCE(pr.pool_refund_withdrawable_reward, 0) 
                          + coalesce(apd.deposit, 0)
                          + COALESCE(rr.withdrawable_reward_rest, 0)
                    ),
                    :snapshot_epoch,
                    NOW()
                  from
                    ss_drep_ranked_delegations rd                      
                    left join ss_pool_rewards r on rd.address = r.address
                    left join ss_gov_pool_refund_rewards pr on rd.address = pr.address
                    left join ss_gov_active_proposal_deposits  apd on apd.return_address = rd.address
                    left join ss_max_slot_balances msb on msb.address = rd.address
                    left join stake_address_balance sab on msb.address = sab.address and msb.max_slot = sab.slot
                    left join ss_gov_spendable_reward_rest rr ON rd.address = rr.address
                    left join stake_registration sd
                                          ON sd.address = rd.address
                                              AND sd.type = 'STAKE_DEREGISTRATION'
                                              AND sd.epoch <= :epoch
                                              AND (
                                                 sd.slot > rd.slot OR
                                                 (sd.slot = rd.slot AND sd.tx_index > rd.tx_index) OR
                                                 (sd.slot = rd.slot AND sd.tx_index = rd.tx_index AND sd.cert_index > rd.cert_index)
                                                 )
                  where
                    (rd.drep_type = 'ABSTAIN' OR rd.drep_type = 'NO_CONFIDENCE') 
                    AND sd.address IS NULL              
                  group by
                    rd.drep_type
                """;

        var params = new MapSqlParameterSource();
        params.addValue("epoch", epoch);
        params.addValue("snapshot_epoch", epoch + 1);
        if (!isInBootstrapPhase) {
            params.addValue("max_bootstrap_phase_epoch", maxBootstrapPhaseEpoch);
        }

        long t1 = System.currentTimeMillis();
        jdbcTemplate.update(query1, params);
        jdbcTemplate.update(query2, params);
        long t2 = System.currentTimeMillis();

        log.info("DRep Stake Distribution snapshot for epoch : {} is taken", epoch);
        log.info(">>>>>>>>>>>>>>>>>>>> DRep Stake Distribution Stake Snapshot taken for epoch : {} <<<<<<<<<<<<<<<<<<<<", epoch);
        log.info("Time taken to take DRep Stake Distribution snapshot for epoch : {} is : {} ms", epoch, (t2 - t1));

    }

    private boolean isPublicNetwork() {
        return storeProperties.getProtocolMagic() == Networks.mainnet().getProtocolMagic()
                || storeProperties.getProtocolMagic() == Networks.preprod().getProtocolMagic()
                || storeProperties.getProtocolMagic() == Networks.preview().getProtocolMagic();
    }
}

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
import com.bloxbean.cardano.yaci.store.governance.storage.GovActionProposalStorage;
import com.bloxbean.cardano.yaci.store.governanceaggr.GovernanceAggrProperties;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.GovActionProposalStatus;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.GovActionProposalStatusStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
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
        jdbcTemplate.update("delete from drep_dist where epoch = :epoch", new MapSqlParameterSource().addValue("epoch", epoch));

        //Drop temp tables in parallel
        List<String> dropQueries = List.of(
            "DROP TABLE IF EXISTS ss_drep_ranked_delegations",
            "DROP TABLE IF EXISTS ss_drep_status",
            "DROP TABLE IF EXISTS ss_gov_active_proposal_deposits"
        );

        for (String query : dropQueries) {
            jdbcTemplate.update(query, Map.of());
        }
        log.info("Dropped existing temp tables for DRep distribution !!!");

        String rankedDelegationsQuery = """
                CREATE TABLE ss_drep_ranked_delegations AS
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
                """;

        String drepStatusQuery = """
                CREATE TABLE ss_drep_status AS
                select
                      drep_id,
                      drep_hash,
                      tx_index,
                      cert_index,
                      status,
                      registration_slot,
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
                """;

        String activeProposalDepositsQuery = """
                CREATE TABLE ss_gov_active_proposal_deposits AS
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
                """;

        List<String> createTableQueries = List.of(
                rankedDelegationsQuery,
                drepStatusQuery,
                activeProposalDepositsQuery
        );

        long start = System.currentTimeMillis();
        var epochParam = new MapSqlParameterSource();
        epochParam.addValue("epoch", epoch);

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
                "CREATE INDEX idx_ss_drep_ranked_delegations_drep_id ON ss_drep_ranked_delegations(drep_id)",
                "CREATE INDEX idx_ss_drep_ranked_delegations_rn ON ss_ranked_delegations(rn)",

                "CREATE INDEX idx_ss_drep_status_drep_id ON ss_drep_status(drep_id)",
                "CREATE INDEX idx_ss_gov_active_proposal_deposits_ret_address ON ss_gov_active_proposal_deposits(return_address)"
        );

        start = System.currentTimeMillis();
        for (String indexQuery : createIndexQueries) {
            jdbcTemplate.update(indexQuery, Map.of());
        }
        end = System.currentTimeMillis();
        log.info(">> Indexes created for DRep dist temp tables <<" + (end - start) + " ms");

        String excludeDelegationByRegistrationSlotCondition = Strings.EMPTY;

        if (!isInBootstrapPhase) {
            excludeDelegationByRegistrationSlotCondition = """
              AND  (
                rd.slot > ds.registration_slot
                or (rd.slot = ds.registration_slot and rd.tx_index > ds.tx_index)
                or (rd.slot = ds.registration_slot and rd.tx_index = ds.tx_index and rd.epoch <= :max_bootstrap_phase_epoch)
                or (rd.slot = ds.registration_slot and rd.tx_index = ds.tx_index and rd.epoch > :max_bootstrap_phase_epoch and rd.cert_index > ds.cert_index)
            )
          """;
        }

        String query1 = """
                  INSERT INTO drep_dist               
                  select
                    rd.drep_hash,
                    rd.drep_id,
                    sum(
                      COALESCE(sab.quantity, 0) 
                          + COALESCE(r.withdrawable_reward, 0) 
                          + COALESCE(pr.pool_refund_withdrawable_reward, 0) 
                          + COALESCE(ir.insta_withdrawable_reward, 0) 
                          + coalesce(apd.deposit, 0) 
                          + COALESCE(rr.withdrawable_reward_rest, 0)
                    ),
                    :epoch,
                    NOW()
                  from
                    ss_drep_ranked_delegations rd
                    left join ss_drep_status ds on rd.drep_id = ds.drep_id
                    and rd.rn = 1
                    left join ss_pool_rewards r on rd.address = r.address
                    left join ss_pool_refund_rewards pr on rd.address = pr.address
                    left join ss_insta_spendable_rewards ir on rd.address = ir.address
                    left join ss_gov_active_proposal_deposits  apd on apd.return_address = rd.address
                    left join ss_max_slot_balances msb on msb.address = rd.address
                    left join stake_address_balance sab on msb.address = sab.address and msb.max_slot = sab.slot
                    left join ss_spendable_reward_rest rr ON rd.address = rr.address
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
                    ds.status = 'ACTIVE'
                    and ds.rn = 1
                    and sd.address IS NULL
                    """ + excludeDelegationByRegistrationSlotCondition + """
                  group by
                    rd.drep_hash,
                    rd.drep_id
                    """;

        String query2 = """
                  INSERT INTO drep_dist
                  select
                    rd.drep_type,
                    null,
                    sum(
                      COALESCE(sab.quantity, 0)
                          + COALESCE(r.withdrawable_reward, 0) 
                          + COALESCE(pr.pool_refund_withdrawable_reward, 0) 
                          + COALESCE(ir.insta_withdrawable_reward, 0) 
                          + coalesce(apd.deposit, 0)
                    ),
                    :epoch,
                    NOW()
                  from
                    ss_drep_ranked_delegations rd                      
                    left join ss_pool_rewards r on rd.address = r.address
                    left join ss_pool_refund_rewards pr on rd.address = pr.address
                    left join ss_insta_spendable_rewards ir on rd.address = ir.address
                    left join ss_gov_active_proposal_deposits  apd on apd.return_address = rd.address
                    left join ss_max_slot_balances msb on msb.address = rd.address
                    left join stake_address_balance sab on msb.address = sab.address and msb.max_slot = sab.slot
                    left join ss_spendable_reward_rest rr ON rd.address = rr.address
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
                    rd.rn=1 AND (rd.drep_type = 'ABSTAIN' OR rd.drep_type = 'NO_CONFIDENCE') 
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

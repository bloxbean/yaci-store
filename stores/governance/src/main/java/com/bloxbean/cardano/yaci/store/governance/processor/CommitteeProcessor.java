
package com.bloxbean.cardano.yaci.store.governance.processor;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.core.model.governance.actions.UpdateCommittee;
import com.bloxbean.cardano.yaci.core.types.UnitInterval;
import com.bloxbean.cardano.yaci.store.client.governance.ProposalStateClient;
import com.bloxbean.cardano.yaci.store.common.aspect.EnableIf;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionProposal;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionStatus;
import com.bloxbean.cardano.yaci.store.common.genesis.ConwayGenesis;
import com.bloxbean.cardano.yaci.store.common.util.StringUtil;
import com.bloxbean.cardano.yaci.store.events.GenesisBlockEvent;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.events.internal.PreAdaPotJobProcessingEvent;
import com.bloxbean.cardano.yaci.store.events.internal.PreEpochTransitionEvent;
import com.bloxbean.cardano.yaci.store.governance.domain.Committee;
import com.bloxbean.cardano.yaci.store.governance.storage.CommitteeStorage;
import com.bloxbean.cardano.yaci.store.governance.storage.CommitteeStorageReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import static com.bloxbean.cardano.yaci.store.common.util.UnitIntervalUtil.safeRatio;
import static com.bloxbean.cardano.yaci.store.governance.GovernanceStoreConfiguration.STORE_GOVERNANCE_ENABLED;

@Component
@RequiredArgsConstructor
@EnableIf(STORE_GOVERNANCE_ENABLED)
@Slf4j
public class CommitteeProcessor {
    private final StoreProperties storeProperties;
    private final CommitteeStorage committeeStorage;
    private final CommitteeStorageReader committeeStorageReader;
    private final ProposalStateClient proposalStateClient;

    @EventListener
    @Transactional
    public void handleEpochChangeEvent(PreEpochTransitionEvent event) {
        if (event.getEra().getValue() < Era.Conway.getValue()) {
            return;
        }

        Era prevEra = event.getPreviousEra();
        Era newEra = event.getEra();
        long protocolMagic = event.getMetadata().getProtocolMagic();
        long slot = event.getMetadata().getSlot();
        int epoch = event.getMetadata().getEpochNumber();

        // store data from genesis file
        if (newEra.equals(Era.Conway) && prevEra != Era.Conway) {
            var latestCommitteeOpt = committeeStorageReader.findByMaxEpoch();
            if (latestCommitteeOpt.isPresent()) {
                log.info("Committee data already exists, skip storing constitution from genesis file");
                return;
            }

            var conwayGenesis = getConwayGenesis(protocolMagic);
            var numerator = conwayGenesis.getCommitteeNumerator();
            var denominator = conwayGenesis.getCommitteeDenominator();
            var threshold = conwayGenesis.getCommitteeThreshold();
            var govActionTxHash = "genesis.conway";
            Committee committee = buildCommittee(govActionTxHash, null, numerator, denominator, threshold, epoch, slot);
            committeeStorage.save(committee);
        }

    }

    private ConwayGenesis getConwayGenesis(long protocolMagic) {
        String conwayGenesisFile = storeProperties.getConwayGenesisFile();

        if (StringUtil.isEmpty(conwayGenesisFile))
            return new ConwayGenesis(protocolMagic);
        else
            return new ConwayGenesis(new File(conwayGenesisFile));
    }

    private Committee buildCommittee(String govActionTxHash, Integer govActionIndex,
                                     BigInteger thresholdNumerator, BigInteger thresholdDenominator, BigDecimal threshold,
                                     int epoch, long slot) {
        return Committee.builder()
                .govActionTxHash(govActionTxHash)
                .govActionIndex(govActionIndex)
                .thresholdNumerator(thresholdNumerator)
                .thresholdDenominator(thresholdDenominator)
                .threshold(threshold)
                .epoch(epoch)
                .slot(slot)
                .build();
    }

    /**
     * This will be invoked for custom devnet like Yaci DevKit devnet which directly starts from Conway
     * @param genesisBlockEvent
     */
    @EventListener
    @Transactional
    public void handleGenesisBlockEvent(GenesisBlockEvent genesisBlockEvent) {
        if (genesisBlockEvent.getEra().getValue() < Era.Conway.getValue())
            return;

        var conwayGenesis = getConwayGenesis(storeProperties.getProtocolMagic());
        var numerator = conwayGenesis.getCommitteeNumerator();
        var denominator = conwayGenesis.getCommitteeDenominator();
        var threshold = conwayGenesis.getCommitteeThreshold();
        var govActionTxHash = "genesis.conway";
        Committee committee = buildCommittee(govActionTxHash, null, numerator, denominator, threshold, genesisBlockEvent.getEpoch(), genesisBlockEvent.getSlot());
        committeeStorage.save(committee);
    }

    @EventListener
    @Transactional
    public void handleRollbackEvent(RollbackEvent rollbackEvent) {
        int count = committeeStorage.deleteBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());
        log.info("Rollback -- {} committee records", count);
    }

    @EventListener
    @Transactional
    public void handlePreAdaPotJobProcessingEvent(PreAdaPotJobProcessingEvent event) {
        int epoch = event.getEpoch();
        long slot = event.getSlot();

        List<GovActionProposal> ratifiedProposalsInPrevEpoch =
                proposalStateClient.getProposalsByStatusAndEpoch(GovActionStatus.RATIFIED, epoch - 1);

        for (var proposal : ratifiedProposalsInPrevEpoch) {
            if (proposal.getGovAction() instanceof UpdateCommittee updateCommittee) {
                UnitInterval quorumThreshold = updateCommittee.getThreshold();

                if (quorumThreshold != null) {
                    Committee committee = buildCommittee(proposal.getTxHash(),
                            proposal.getIndex(),
                            quorumThreshold.getNumerator(),
                            quorumThreshold.getDenominator(),
                            safeRatio(quorumThreshold),
                            epoch,
                            slot);

                    committeeStorage.save(committee);
                }
            }
        }
    }
}

package com.bloxbean.cardano.yaci.store.governance.processor;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.core.model.governance.Anchor;
import com.bloxbean.cardano.yaci.core.model.governance.actions.NewConstitution;
import com.bloxbean.cardano.yaci.store.client.governance.ProposalStateClient;
import com.bloxbean.cardano.yaci.store.common.aspect.EnableIf;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.domain.GenesisConstitution;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionProposal;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionStatus;
import com.bloxbean.cardano.yaci.store.common.genesis.ConwayGenesis;
import com.bloxbean.cardano.yaci.store.common.util.StringUtil;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.events.internal.PreAdaPotJobProcessingEvent;
import com.bloxbean.cardano.yaci.store.events.internal.PreEpochTransitionEvent;
import com.bloxbean.cardano.yaci.store.governance.domain.Constitution;
import com.bloxbean.cardano.yaci.store.governance.storage.ConstitutionStorage;
import com.bloxbean.cardano.yaci.store.governance.storage.ConstitutionStorageReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.List;

import static com.bloxbean.cardano.yaci.store.governance.GovernanceStoreConfiguration.STORE_GOVERNANCE_ENABLED;

@Component
@RequiredArgsConstructor
@EnableIf(STORE_GOVERNANCE_ENABLED)
@Slf4j
public class ConstitutionProcessor {
    private final StoreProperties storeProperties;
    private final ConstitutionStorage constitutionStorage;
    private final ConstitutionStorageReader constitutionStorageReader;
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
            boolean isConstitutionDataPresent = constitutionStorageReader.findCurrentConstitution().isPresent();
            if (isConstitutionDataPresent) {
                log.debug("Constitution already exists. Skipping storing constitution from genesis file");
                return;
            }
            var constitution = getGenesisConstitution(protocolMagic);
            if (constitution != null) {
                var constitutionToSave = buildConstitution(constitution, epoch, slot);
                constitutionStorage.save(constitutionToSave);
            }
        }
    }

    private GenesisConstitution getGenesisConstitution(long protocolMagic) {
        String conwayGenesisFile = storeProperties.getConwayGenesisFile();

        if (StringUtil.isEmpty(conwayGenesisFile))
            return new ConwayGenesis(protocolMagic).getConstitution();
        else
            return new ConwayGenesis(new File(conwayGenesisFile)).getConstitution();
    }

    private Constitution buildConstitution(GenesisConstitution genesisConstitution, int activeEpoch, long slot) {
        return Constitution.builder()
                .anchorUrl(genesisConstitution.getAnchorUrl())
                .anchorHash(genesisConstitution.getAnchorHash())
                .activeEpoch(activeEpoch)
                .script(genesisConstitution.getScript())
                .slot(slot)
                .build();
    }

    @EventListener
    @Transactional
    public void handlePreAdaPotJobProcessingEvent(PreAdaPotJobProcessingEvent event) {
        int epoch = event.getEpoch();
        long slot = event.getSlot();

        List<GovActionProposal> ratifiedProposalsInPrevEpoch =
                proposalStateClient.getProposalsByStatusAndEpoch(GovActionStatus.RATIFIED, epoch - 1);

        for (var proposal : ratifiedProposalsInPrevEpoch) {
            if (proposal.getGovAction() instanceof NewConstitution newConstitution) {
                Anchor anchor = newConstitution.getConstitution().getAnchor();

                var constitutionToSave = Constitution.builder()
                        .anchorUrl(anchor != null ? anchor.getAnchor_url() : null)
                        .anchorHash(anchor != null ? anchor.getAnchor_data_hash() : null)
                        .activeEpoch(epoch)
                        .script(newConstitution.getConstitution().getScripthash())
                        .slot(slot)
                        .build();

                constitutionStorage.save(constitutionToSave);
            }
        }
    }

    @EventListener
    @Transactional
    //TODO -- tests
    public void handleRollbackEvent(RollbackEvent rollbackEvent) {
        int count = constitutionStorage.deleteBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());
        log.info("Rollback -- {} constitution records", count);
    }
}

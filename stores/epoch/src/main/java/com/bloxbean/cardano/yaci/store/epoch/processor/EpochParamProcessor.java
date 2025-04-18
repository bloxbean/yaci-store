package com.bloxbean.cardano.yaci.store.epoch.processor;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.core.model.ProtocolParamUpdate;
import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.core.model.governance.actions.HardForkInitiationAction;
import com.bloxbean.cardano.yaci.core.model.governance.actions.ParameterChangeAction;
import com.bloxbean.cardano.yaci.store.client.governance.ProposalStateClient;
import com.bloxbean.cardano.yaci.store.common.aspect.EnableIf;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionProposal;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionStatus;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.common.util.JsonUtil;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import com.bloxbean.cardano.yaci.store.epoch.domain.EpochParam;
import com.bloxbean.cardano.yaci.store.epoch.domain.ProtocolParamsProposal;
import com.bloxbean.cardano.yaci.store.epoch.mapper.DomainMapper;
import com.bloxbean.cardano.yaci.store.epoch.storage.EpochParamStorage;
import com.bloxbean.cardano.yaci.store.epoch.storage.ProtocolParamsProposalStorage;
import com.bloxbean.cardano.yaci.store.events.GenesisBlockEvent;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.events.internal.PreAdaPotJobProcessingEvent;
import com.bloxbean.cardano.yaci.store.events.internal.PreEpochTransitionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.bloxbean.cardano.yaci.store.epoch.EpochStoreConfiguration.STORE_EPOCH_ENABLED;

@Component
@RequiredArgsConstructor
@EnableIf(STORE_EPOCH_ENABLED)
@Slf4j
public class EpochParamProcessor {
    private final EpochParamStorage epochParamStorage;
    private final ProtocolParamsProposalStorage protocolParamsProposalStorage;
    private final EraGenesisProtocolParamsUtil eraGenesisProtocolParamsUtil;
    private final ProposalStateClient proposalStateClient;
    private final EraService eraService;
    private final StoreProperties storeProperties;
    private final DomainMapper mapper = DomainMapper.INSTANCE;
    private PPEraChangeRules ppEraChangeRules = new PPEraChangeRules();

    /**
     * This is mostly be used for custom networks or test networks which start directly from a non-byron era
     * @param genesisBlockEvent
     */
    @EventListener
    @Transactional
    public void handleGenesisBlockEvent(GenesisBlockEvent genesisBlockEvent) {
        Era startEra = genesisBlockEvent.getEra();
        ProtocolParams genesisProtocolParams = eraGenesisProtocolParamsUtil
                .getGenesisProtocolParameters(startEra, null, genesisBlockEvent.getProtocolMagic())
                .orElse(null);

        if (genesisProtocolParams != null) {
            log.info("Network is starting with the following protocol params : \n" + genesisProtocolParams);

            EpochParam epochParam = EpochParam.builder()
                    .epoch(genesisBlockEvent.getEpoch())
                    .params(genesisProtocolParams)
                    .slot(genesisBlockEvent.getSlot())
                    .blockNumber(genesisBlockEvent.getBlock())
                    .blockTime(genesisBlockEvent.getBlockTime())
                    .build();

            epochParamStorage.save(epochParam);
        } else {
            log.info("No protocol parameters found for genesis block {}", startEra);
        }
    }

    @EventListener
    @Transactional
    public void handleEpochChangeEvent(PreEpochTransitionEvent epochChangeEvent) {
        Integer dbEpoch = epochParamStorage.getMaxEpoch();
        if (epochChangeEvent.getPreviousEpoch() == null && dbEpoch == epochChangeEvent.getEpoch()) {
            log.info("EpochParam info is already there. Let's ignore it.");
            return;
        }

        int newEpoch = epochChangeEvent.getEpoch();
        if ( dbEpoch != null && newEpoch != dbEpoch + 1) {
            log.warn("Some consistency issue. New epoch {}, previously procesed epoch {}", newEpoch, dbEpoch);
            return;
        }

        //handle epoch param
        //check if this new Era has a genesis file, if yes get default protocol params
        Era prevEra = epochChangeEvent.getPreviousEra();
        Era newEra = epochChangeEvent.getEra();

        //Get genesis protocol params
        long protocolMagic = epochChangeEvent.getMetadata().getProtocolMagic();

        if (newEra.getValue() >= Era.Conway.value) {
            log.info("Conway or post Conway era. Epoch param will be processed during adapot job");
            return;
        }

        EpochParam epochParam = resolveEpochParam(protocolMagic, newEra, prevEra, newEpoch,
                epochChangeEvent.getMetadata().getSlot(),
                epochChangeEvent.getMetadata().getBlock(),
                epochChangeEvent.getMetadata().getBlockTime());

        epochParamStorage.save(epochParam);
    }

    @EventListener
    @Transactional
    public void handlePreAdaPotJobProcessingEvent(PreAdaPotJobProcessingEvent event) {
        log.info("Processing epoch param for epoch {} and slot {} during pre-adapot job processing >>>", event.getEpoch(), event.getSlot());
        int epoch = event.getEpoch();
        long slot = event.getSlot();
        long block = event.getBlock();

        int prevEpoch = epoch - 1;
        Era era = eraService.getEraForEpoch(epoch);
        Era prevEra = eraService.getEraForEpoch(prevEpoch);

        if (era.value < Era.Conway.value) {
            log.info("Pre-conway era. Epoch param will be processed during epoch change event");
            return;
        }

        var dbEpochParam = epochParamStorage.getProtocolParams(epoch);
        if (dbEpochParam.isPresent()) {
            log.warn("Epoch param for epoch {} already exists. Ignoring it.", epoch);
            return;
        }

        long blockTime = eraService.blockTime(era, slot);
        long protocolMagic = storeProperties.getProtocolMagic();

        EpochParam resolvedEpochParam = resolveEpochParam(protocolMagic, era, prevEra, epoch, slot,  block, blockTime);
        epochParamStorage.save(resolvedEpochParam);
    }

    private EpochParam resolveEpochParam(long protocolMagic, Era newEra, Era prevEra, int newEpoch, Long slot, Long block, Long blockTime) {
        Optional<ProtocolParams> genesisProtocolParams = eraGenesisProtocolParamsUtil
                .getGenesisProtocolParameters(newEra, prevEra, protocolMagic);

        ProtocolParams protocolParams = new ProtocolParams();

        //Get previous era protocol prams
        EpochParam previousEpochParam = epochParamStorage.getProtocolParams(newEpoch - 1).orElse(null);

        //Get protocol param proposals target for this era (Signature check for validity -- //TODO)
        List<ProtocolParamsProposal> ppProposals = protocolParamsProposalStorage.getProtocolParamsProposalsByTargetEpoch(newEpoch - 1);

        if (previousEpochParam != null)
            protocolParams.merge(previousEpochParam.getParams());

        genesisProtocolParams.ifPresent(protocolParams::merge);

        ppProposals.forEach(ppProposal -> protocolParams.merge(ppProposal.getParams()));

        //Additional Era change specific rules
        ppEraChangeRules.apply(newEra, prevEra, protocolParams);

        // handle parameter change from gov action proposal
        if (newEra.getValue() >= Era.Conway.getValue()) {
            List<GovActionProposal> ratifiedProposalsInPrevEpoch =
                    proposalStateClient.getProposalsByStatusAndEpoch(GovActionStatus.RATIFIED, newEpoch - 1);

            for (var proposal : ratifiedProposalsInPrevEpoch) {
                if (proposal.getGovAction() != null) {
                    if (proposal.getGovAction().getType() == GovActionType.PARAMETER_CHANGE_ACTION) {
                        ParameterChangeAction parameterChangeAction = (ParameterChangeAction) proposal.getGovAction();
                        ProtocolParamUpdate protocolParamUpdate = parameterChangeAction.getProtocolParamUpdate();

                        if (protocolParamUpdate != null)
                            protocolParams.merge(mapper.toProtocolParams(protocolParamUpdate));
                    } else if (proposal.getGovAction().getType() == GovActionType.HARD_FORK_INITIATION_ACTION) {
                        HardForkInitiationAction hardForkInitiationAction = (HardForkInitiationAction) proposal.getGovAction();

                        if (hardForkInitiationAction.getProtocolVersion() != null) {
                            protocolParams.setProtocolMajorVer((int) hardForkInitiationAction.getProtocolVersion().get_1());
                            protocolParams.setProtocolMinorVer((int) hardForkInitiationAction.getProtocolVersion().get_2());
                        }
                    }
                }
            }
        }

        //merge protocol params
        if (log.isDebugEnabled())
            log.debug("Final pp: \n " + JsonUtil.getJson(protocolParams));

        EpochParam epochParam = EpochParam.builder()
                .epoch(newEpoch)
                .params(protocolParams)
                .slot(slot)
                .blockNumber(block)
                .blockTime(blockTime)
                .build();
        return epochParam;
    }

    @EventListener
    @Transactional
    public void handleRollBack(RollbackEvent rollbackEvent) {
        int count = epochParamStorage.deleteBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());
        log.info("Rollback -- {} EpochParam records", count);
    }
}

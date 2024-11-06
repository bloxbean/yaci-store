package com.bloxbean.cardano.yaci.store.epoch.processor;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.common.util.JsonUtil;
import com.bloxbean.cardano.yaci.store.epoch.domain.EpochParam;
import com.bloxbean.cardano.yaci.store.epoch.domain.ProtocolParamsProposal;
import com.bloxbean.cardano.yaci.store.epoch.storage.EpochParamStorage;
import com.bloxbean.cardano.yaci.store.epoch.storage.ProtocolParamsProposalStorage;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.events.internal.PreEpochTransitionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class EpochParamProcessor {
    private final EpochParamStorage epochParamStorage;
    private final ProtocolParamsProposalStorage protocolParamsProposalStorage;
    private final EraGenesisProtocolParamsUtil eraGenesisProtocolParamsUtil;

    private PPEraChangeRules ppEraChangeRules = new PPEraChangeRules();

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
        Optional<ProtocolParams> genesisProtocolParams = eraGenesisProtocolParamsUtil
                .getGenesisProtocolParameters(newEra, prevEra, epochChangeEvent.getMetadata().getProtocolMagic());

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

        //merge protocol params
        if (log.isDebugEnabled())
            log.debug("Final pp: \n " + JsonUtil.getJson(protocolParams));

        EpochParam epochParam = EpochParam.builder()
                .epoch(newEpoch)
                .params(protocolParams)
                .slot(epochChangeEvent.getMetadata().getSlot())
                .blockNumber(epochChangeEvent.getMetadata().getBlock())
                .blockTime(epochChangeEvent.getMetadata().getBlockTime())
                .build();

        epochParamStorage.save(epochParam);
    }

    @EventListener
    @Transactional
    public void handleRollBack(RollbackEvent rollbackEvent) {
        int count = epochParamStorage.deleteBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());
        log.info("Rollback -- {} EpochParam records", count);
    }
}

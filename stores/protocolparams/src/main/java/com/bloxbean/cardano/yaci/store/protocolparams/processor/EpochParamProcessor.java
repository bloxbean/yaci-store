package com.bloxbean.cardano.yaci.store.protocolparams.processor;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.common.genesis.AlonzoGenesis;
import com.bloxbean.cardano.yaci.store.common.genesis.ShelleyGenesis;
import com.bloxbean.cardano.yaci.store.common.util.JsonUtil;
import com.bloxbean.cardano.yaci.store.events.EpochChangeEvent;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.protocolparams.domain.EpochParam;
import com.bloxbean.cardano.yaci.store.protocolparams.domain.ProtocolParamsProposal;
import com.bloxbean.cardano.yaci.store.protocolparams.storage.api.EpochParamStorage;
import com.bloxbean.cardano.yaci.store.protocolparams.storage.api.ProtocolParamsProposalStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class EpochParamProcessor {
    private final EpochParamStorage epochParamStorage;
    private final ProtocolParamsProposalStorage protocolParamsProposalStorage;

    @EventListener
    @Transactional
    public void handleEpochChangeEvent(EpochChangeEvent epochChangeEvent) {
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

        ProtocolParams genesisProtocolParams = null;
        if (newEra != prevEra) {
            //Get default protocol params if any
            if (newEra == Era.Shelley) {
                genesisProtocolParams = new ShelleyGenesis(epochChangeEvent.getEventMetadata().getProtocolMagic()).getProtocolParams();
            } else if (newEra == Era.Alonzo) {
                genesisProtocolParams = new AlonzoGenesis(epochChangeEvent.getEventMetadata().getProtocolMagic()).getProtocolParams();
            }
        }

        ProtocolParams protocolParams = new ProtocolParams();

        //Get previous era protocol prams
        EpochParam previousEpochParam = epochParamStorage.getProtocolParams(newEpoch - 1).orElse(null);

        //Get protocol param proposals target for this era (Signature check for validity -- //TODO)
        List<ProtocolParamsProposal> ppProposals = protocolParamsProposalStorage.getProtocolParamsProposalsByTargetEpoch(newEpoch);

        //Also check any proposal target to previous epoch, but also submitted in previous epoch. Those should be considered now.
        //Filter to check if submitted in previous epochl
        List<ProtocolParamsProposal> ppProposalsSubmittedPrevEpoch = protocolParamsProposalStorage.getProtocolParamsProposalsByTargetEpoch(newEpoch - 1)
                .stream().filter(protocolParamsProposal -> protocolParamsProposal.getEpoch() == newEpoch - 1).toList();

        if (previousEpochParam != null)
            protocolParams.merge(previousEpochParam.getParams());
        if (genesisProtocolParams != null)
            protocolParams.merge(genesisProtocolParams);

        ppProposalsSubmittedPrevEpoch.forEach(ppProposal -> protocolParams.merge(ppProposal.getParams()));
        ppProposals.forEach(ppProposal -> protocolParams.merge(ppProposal.getParams()));

        //merge protocol params
        if (log.isDebugEnabled())
            log.debug("Final pp: \n " + JsonUtil.getJson(protocolParams));

        EpochParam epochParam = EpochParam.builder()
                .epoch(newEpoch)
                .params(protocolParams)
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

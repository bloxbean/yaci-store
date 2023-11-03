package com.bloxbean.cardano.yaci.store.protocolparams.processor;

import com.bloxbean.cardano.yaci.core.model.ProtocolParamUpdate;
import com.bloxbean.cardano.yaci.core.model.Update;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.events.UpdateEvent;
import com.bloxbean.cardano.yaci.store.events.domain.TxUpdate;
import com.bloxbean.cardano.yaci.store.protocolparams.domain.ProtocolParamsProposal;
import com.bloxbean.cardano.yaci.store.protocolparams.mapper.DomainMapper;
import com.bloxbean.cardano.yaci.store.protocolparams.storage.api.ProtocolParamsProposalStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProtocolParamsUpdateProcessor {
    private final ProtocolParamsProposalStorage protocolParamsProposalStorage;
    private final DomainMapper mapper = DomainMapper.INSTANCE;

    @EventListener
    @Transactional
    public void handleUpdateEvent(UpdateEvent updateEvent) {
        List<TxUpdate> txUpdates = updateEvent.getUpdates();
        if (txUpdates == null || txUpdates.size() == 0)
            return;

        EventMetadata metadata = updateEvent.getMetadata();

        var protocolParamsProposals = txUpdates.stream()
                .map(txUpdate -> {
                    Update update = txUpdate.getUpdate();
                    Map<String, ProtocolParamUpdate> ppUpdates = update.getProtocolParamUpdates();
                    if (ppUpdates.size() == 0)
                        return Collections.EMPTY_LIST;

                    return ppUpdates.entrySet().stream()
                            .map(ppEntry -> ProtocolParamsProposal.builder()
                                    .txHash(txUpdate.getTxHash())
                                    .keyHash(ppEntry.getKey())
                                    .params(mapper.toProtocolParams(ppEntry.getValue()))
                                    .targetEpoch((int) update.getEpoch())
                                    .epoch(metadata.getEpochNumber())
                                    .slot(metadata.getSlot())
                                    .era(metadata.getEra())
                                    .blockNumber(metadata.getBlock())
                                    .blockTime(metadata.getBlockTime())
                                    .build()).toList();
                }).filter(list -> !list.isEmpty())
                .flatMap(List::stream)
                .toList();

        if (!protocolParamsProposals.isEmpty())
            protocolParamsProposalStorage.saveAll(protocolParamsProposals);
    }

    @EventListener
    @Transactional
    public void handleRollback(RollbackEvent rollbackEvent) {
        int count = protocolParamsProposalStorage.deleteBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());

        log.info("Rollback -- {} ProtocolParamsProposal records", count);
    }

}

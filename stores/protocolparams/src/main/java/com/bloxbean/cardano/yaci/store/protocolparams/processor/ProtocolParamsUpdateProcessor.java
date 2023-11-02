package com.bloxbean.cardano.yaci.store.protocolparams.processor;

import co.nstant.in.cbor.model.Array;
import co.nstant.in.cbor.model.DataItem;
import co.nstant.in.cbor.model.Special;
import co.nstant.in.cbor.model.UnsignedInteger;
import com.bloxbean.cardano.yaci.core.model.ProtocolParamUpdate;
import com.bloxbean.cardano.yaci.core.model.Update;
import com.bloxbean.cardano.yaci.core.util.CborSerializationUtil;
import com.bloxbean.cardano.yaci.core.util.HexUtil;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.common.genesis.util.PlutusKeys;
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

import java.math.BigInteger;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProtocolParamsUpdateProcessor {
    private final ProtocolParamsProposalStorage protocolParamsProposalStorage;
    private final DomainMapper mapper;

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
                                    .params(convertYaciProtocolParamUpdateToDomain(ppEntry.getValue()))
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

    private ProtocolParams convertYaciProtocolParamUpdateToDomain(ProtocolParamUpdate protocolParamUpdate) {
        ProtocolParams protocolParams = mapper.toProtocolParams(protocolParamUpdate);
        var updatedCostModelMap = getCostModels(protocolParamUpdate);
        if (updatedCostModelMap != null && updatedCostModelMap.size() > 0) {
            protocolParams.setCostModels(updatedCostModelMap);
        }

        return protocolParams;
    }

    private Map<String, long[]> getCostModels(ProtocolParamUpdate protocolParamUpdate) {
        var costModelmap = protocolParamUpdate.getCostModels();
        if (costModelmap == null)
            return null;

        Map<String, long[]> resultMap = new HashMap<>();
        var languageKeys = costModelmap.keySet();
        for (var key : languageKeys) {
            String strKey = null;
            if (key == 0) {
                strKey = PlutusKeys.PLUTUS_V1;
            } else if (key == 1) {
                strKey = PlutusKeys.PLUTUS_V2;
            } else if (key == 2) {
                strKey = PlutusKeys.PLUTUS_V3;
            }

            var cborCostModelValue = costModelmap.get(key);
            Array array = (Array) CborSerializationUtil.deserializeOne(HexUtil.decodeHexString(cborCostModelValue));
            List<Long> costs = new ArrayList<>();
            for (DataItem di : array.getDataItems()) {
                if (di == Special.BREAK)
                    continue;
                BigInteger val = ((UnsignedInteger) di).getValue();
                costs.add(val.longValue());
            }

            long[] costArray = costs.stream()
                    .mapToLong(Long::longValue)
                    .toArray();

            resultMap.put(strKey, costArray);
        }

        return resultMap;
    }

    @EventListener
    @Transactional
    public void handleRollback(RollbackEvent rollbackEvent) {
        int count = protocolParamsProposalStorage.deleteBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());

        log.info("Rollback -- {} ProtocolParamsProposal records", count);
    }

}

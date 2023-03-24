package com.bloxbean.cardano.yaci.store.metadata.processor;

import com.bloxbean.cardano.yaci.store.common.util.StringUtil;
import com.bloxbean.cardano.yaci.store.events.AuxDataEvent;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.metadata.model.TxMetadataLabelEntity;
import com.bloxbean.cardano.yaci.store.metadata.repository.TxMetadataLabelRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class MetadataProcessor {
    private final TxMetadataLabelRepository metadataLabelRepository;
    private ObjectMapper objectMapper = new ObjectMapper();

    @EventListener
    @Transactional
    public void handleAuxDataEvent(AuxDataEvent auxDataEvent) {
        if (log.isDebugEnabled())
            log.debug("Received AuxDataEvent");

        EventMetadata eventMetadata = auxDataEvent.getMetadata();
        List<TxMetadataLabelEntity> txMetadataLabelEntities = auxDataEvent.getTxAuxDataList().stream()
                .filter(txAuxDataEvent -> txAuxDataEvent.getAuxData() != null
                        && !StringUtil.isEmpty(txAuxDataEvent.getAuxData().getMetadataJson()))
                .map(txAuxData -> {
                    String json = txAuxData.getAuxData().getMetadataJson();
                    JsonNode jsonNode;
                    try {
                        jsonNode = objectMapper.readTree(json);
                    } catch (JsonProcessingException e) {
                        throw new IllegalStateException(e);
                    }

                    List<TxMetadataLabelEntity> txMetadataLabels = new ArrayList<>();
                    jsonNode.fieldNames().forEachRemaining(fieldName -> {
                        TxMetadataLabelEntity txMetadataLabel = TxMetadataLabelEntity.builder()
                                .slot(eventMetadata.getSlot())
                                .txHash(txAuxData.getTxHash())
                                .label(fieldName)
                                .body((jsonNode.get(fieldName)).toString())
                                .build();

                        txMetadataLabels.add(txMetadataLabel);
                    });

                    return txMetadataLabels;
                })
                .flatMap(txMetadataLabelList -> txMetadataLabelList.stream())
                .collect(Collectors.toList());

                if (txMetadataLabelEntities.size() > 0) {
                    if (log.isDebugEnabled())
                        log.debug("Saving metadata >> Length : " + txMetadataLabelEntities.size());
                    metadataLabelRepository.saveAll(txMetadataLabelEntities);
                }
    }
}

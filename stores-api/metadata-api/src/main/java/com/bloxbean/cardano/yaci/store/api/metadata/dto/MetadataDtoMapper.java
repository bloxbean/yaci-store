package com.bloxbean.cardano.yaci.store.api.metadata.dto;

import com.bloxbean.cardano.client.util.JsonUtil;
import com.bloxbean.cardano.yaci.store.metadata.domain.TxMetadataLabel;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
@Slf4j
public abstract class MetadataDtoMapper {
    public TxMetadataLabelDto toTxMetadataLabelDto(@NonNull TxMetadataLabel txMetadataLabel) {
        JsonNode jsonNode = null;
        try {
            jsonNode = JsonUtil.parseJson(txMetadataLabel.getBody());
        } catch (Exception e) {
            log.error("error parsing metadata : " + txMetadataLabel.getBody());
        }

        return TxMetadataLabelDto.builder()
                .label(txMetadataLabel.getLabel())
                .body(jsonNode)
                .jsonMetadata(jsonNode)
                .slot(txMetadataLabel.getSlot())
                .blockNumber(txMetadataLabel.getBlockNumber())
                .blockTime(txMetadataLabel.getBlockTime())
                .build();
    }

    public TxMetadataLabelCBORDto toTxMetadataLabelCBORDto(@NonNull TxMetadataLabel txMetadataLabel) {
        return TxMetadataLabelCBORDto.builder()
                .label(txMetadataLabel.getLabel())
                .metadata(txMetadataLabel.getCbor())
                .slot(txMetadataLabel.getSlot())
                .blockNumber(txMetadataLabel.getBlockNumber())
                .blockTime(txMetadataLabel.getBlockTime())
                .build();
    }

    public MetadataLabelDto toMetadataLabelDto(@NonNull TxMetadataLabel txMetadataLabel) {
        JsonNode jsonNode = null;
        try {
            jsonNode = JsonUtil.parseJson(txMetadataLabel.getBody());
        } catch (Exception e) {
            log.error("error parsing metadata : " + txMetadataLabel.getBody());
        }

        return MetadataLabelDto.builder()
                .txHash(txMetadataLabel.getTxHash())
                .jsonMetadata(jsonNode)
                .cborMetadata(txMetadataLabel.getCbor())
                .slot(txMetadataLabel.getSlot())
                .blockNumber(txMetadataLabel.getBlockNumber())
                .blockTime(txMetadataLabel.getBlockTime())
                .build();
    }
}

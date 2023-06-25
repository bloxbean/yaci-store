package com.bloxbean.cardano.yaci.store.metadata.dto;

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
                .build();
    }
}

package com.bloxbean.cardano.yaci.store.blockfrost.metadata.mapper;

import com.bloxbean.cardano.yaci.store.blockfrost.metadata.dto.BFMetadataCborDto;
import com.bloxbean.cardano.yaci.store.blockfrost.metadata.dto.BFMetadataJsonDto;
import com.bloxbean.cardano.yaci.store.metadata.domain.TxMetadataLabel;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.Mapper;


@Mapper(componentModel = "spring")
@Slf4j
public abstract class BFMetadataMapper {

    
    public BFMetadataJsonDto toJsonDto(TxMetadataLabel metadata) {
        if (metadata == null) {
            return null;
        }

        String jsonMetadata = null;
        if (metadata.getBody() != null && !metadata.getBody().isBlank()) {
            jsonMetadata = metadata.getBody();
        }

        return BFMetadataJsonDto.builder()
                .txHash(metadata.getTxHash())
                .jsonMetadata(jsonMetadata)
                .build();
    }

    
    public BFMetadataCborDto toCborDto(TxMetadataLabel metadata) {
        if (metadata == null) {
            return null;
        }

        String cbor = metadata.getCbor();
        String cborMetadata = null;
        String rawMetadata = null;

        if (cbor != null && !cbor.isBlank()) {
            rawMetadata = cbor;
            cborMetadata = "\\x" + cbor;
        }

        return BFMetadataCborDto.builder()
                .txHash(metadata.getTxHash())
                .cborMetadata(cborMetadata)
                .metadata(rawMetadata)
                .build();
    }
}

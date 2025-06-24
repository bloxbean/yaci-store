package com.bloxbean.cardano.yaci.store.metadata.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.metadata.domain.TxMetadataLabel;
import com.bloxbean.cardano.yaci.store.metadata.storage.impl.model.TxMetadataLabelEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-20T15:09:15+0100",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.v20250514-1000, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class MetadataMapperImpl extends MetadataMapper {

    @Override
    public TxMetadataLabelEntity toTxMetadataLabelEntity(TxMetadataLabel txMetadataLabel) {
        if ( txMetadataLabel == null ) {
            return null;
        }

        TxMetadataLabelEntity.TxMetadataLabelEntityBuilder<?, ?> txMetadataLabelEntity = TxMetadataLabelEntity.builder();

        txMetadataLabelEntity.blockNumber( txMetadataLabel.getBlockNumber() );
        txMetadataLabelEntity.blockTime( txMetadataLabel.getBlockTime() );
        txMetadataLabelEntity.body( txMetadataLabel.getBody() );
        txMetadataLabelEntity.cbor( txMetadataLabel.getCbor() );
        txMetadataLabelEntity.label( txMetadataLabel.getLabel() );
        txMetadataLabelEntity.slot( txMetadataLabel.getSlot() );
        txMetadataLabelEntity.txHash( txMetadataLabel.getTxHash() );

        return txMetadataLabelEntity.build();
    }

    @Override
    public TxMetadataLabel toTxMetadataLabel(TxMetadataLabelEntity entity) {
        if ( entity == null ) {
            return null;
        }

        TxMetadataLabel.TxMetadataLabelBuilder<?, ?> txMetadataLabel = TxMetadataLabel.builder();

        txMetadataLabel.blockNumber( entity.getBlockNumber() );
        txMetadataLabel.blockTime( entity.getBlockTime() );
        txMetadataLabel.body( entity.getBody() );
        txMetadataLabel.cbor( entity.getCbor() );
        txMetadataLabel.label( entity.getLabel() );
        txMetadataLabel.slot( entity.getSlot() );
        txMetadataLabel.txHash( entity.getTxHash() );

        return txMetadataLabel.build();
    }
}

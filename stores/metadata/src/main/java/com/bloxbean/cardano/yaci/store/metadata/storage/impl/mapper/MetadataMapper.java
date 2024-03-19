package com.bloxbean.cardano.yaci.store.metadata.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.metadata.domain.TxMetadataLabel;
import com.bloxbean.cardano.yaci.store.metadata.storage.impl.model.TxMetadataLabelEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class MetadataMapper {
    public abstract TxMetadataLabelEntity toTxMetadataLabelEntity(TxMetadataLabel txMetadataLabel);
    public abstract TxMetadataLabel toTxMetadataLabel(TxMetadataLabelEntity entity);
}

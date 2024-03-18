package com.bloxbean.cardano.yaci.store.metadata.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.metadata.domain.TxMetadataLabel;
import com.bloxbean.cardano.yaci.store.metadata.storage.impl.model.TxMetadataLabelEntityJpa;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class MetadataMapper {
    public abstract TxMetadataLabelEntityJpa toTxMetadataLabelEntity(TxMetadataLabel txMetadataLabel);
    public abstract TxMetadataLabel toTxMetadataLabel(TxMetadataLabelEntityJpa entity);
}

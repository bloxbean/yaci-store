package com.bloxbean.cardano.yaci.store.blockfrost.asset.mapper;

import com.bloxbean.cardano.yaci.store.blockfrost.asset.dto.BFAssetDTO;
import com.bloxbean.cardano.yaci.store.assets.storage.impl.model.TxAssetInfo;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
@DecoratedWith(BFAssetMapperDecorator.class)
public interface BFAssetMapper {

    BFAssetMapper INSTANCE = Mappers.getMapper(BFAssetMapper.class);

    @Mapping(target = "asset", source = "unit")
    @Mapping(target = "quantity", ignore = true)
    BFAssetDTO toBFAssetDTO(TxAssetInfo txAssetInfo);
}



package com.bloxbean.cardano.yaci.store.blockfrost.asset.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface BFAssetMapper {

    BFAssetMapper INSTANCE = Mappers.getMapper(BFAssetMapper.class);

}



package com.bloxbean.cardano.yaci.store.assets.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.assets.domain.TxAsset;
import com.bloxbean.cardano.yaci.store.assets.storage.impl.model.TxAssetEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class AssetMapper {
    public abstract TxAsset toTxAsset(TxAssetEntity txAssetEntity);
    public abstract TxAssetEntity toTxAssetEntity(TxAsset txAsset);
}

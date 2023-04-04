package com.bloxbean.cardano.yaci.store.assets.storage;

import com.bloxbean.cardano.yaci.store.assets.domain.TxAsset;
import com.bloxbean.cardano.yaci.store.assets.storage.model.TxAssetEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class AssetMapper {
    public abstract TxAsset toTxAsset(TxAssetEntity txAssetEntity);
    public abstract TxAssetEntity toTxAssetEntity(TxAsset txAsset);
}

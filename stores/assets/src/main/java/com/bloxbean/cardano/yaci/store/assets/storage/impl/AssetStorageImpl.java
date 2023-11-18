package com.bloxbean.cardano.yaci.store.assets.storage.impl;

import com.bloxbean.cardano.yaci.store.assets.domain.TxAsset;
import com.bloxbean.cardano.yaci.store.assets.storage.AssetStorage;
import com.bloxbean.cardano.yaci.store.assets.storage.impl.mapper.AssetMapper;
import com.bloxbean.cardano.yaci.store.assets.storage.impl.model.TxAssetEntity;
import com.bloxbean.cardano.yaci.store.assets.storage.impl.repository.TxAssetRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class AssetStorageImpl implements AssetStorage {
    private final TxAssetRepository txAssetRepository;
    private final AssetMapper assetMapper;

    @Override
    public void saveAll(List<TxAsset> txAssetList) {
        List<TxAssetEntity> txAssetEntities = txAssetList.stream().map(assetMapper::toTxAssetEntity).toList();
        txAssetRepository.saveAll(txAssetEntities);
    }

    @Override
    public int deleteBySlotGreaterThan(long slot) {
        return txAssetRepository.deleteBySlotGreaterThan(slot);
    }

}

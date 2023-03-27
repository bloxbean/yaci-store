package com.bloxbean.cardano.yaci.store.assets.storage;

import com.bloxbean.cardano.yaci.store.assets.domain.TxAsset;
import com.bloxbean.cardano.yaci.store.assets.storage.model.TxAssetEntity;
import com.bloxbean.cardano.yaci.store.assets.storage.repository.TxAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
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
    public List<TxAsset> findByTxHash(String txHash) {
        List<TxAssetEntity> txAssetEntities = txAssetRepository.findByTxHash(txHash);
        return txAssetEntities.stream().map(assetMapper::toTxAsset).toList();
    }

    @Override
    public int deleteBySlotGreaterThan(long slot) {
        return txAssetRepository.deleteBySlotGreaterThan(slot);
    }

}

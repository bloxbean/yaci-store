package com.bloxbean.cardano.yaci.store.assets.storage;

import com.bloxbean.cardano.yaci.store.assets.domain.TxAsset;

import java.util.List;

public interface AssetStorage {
    int deleteBySlotGreaterThan(long slot);

    void saveAll(List<TxAsset> txAssetList);

    List<TxAsset> findByTxHash(String txHash);
}

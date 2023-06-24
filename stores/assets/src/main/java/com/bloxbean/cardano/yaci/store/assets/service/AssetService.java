package com.bloxbean.cardano.yaci.store.assets.service;

import com.bloxbean.cardano.yaci.store.assets.domain.TxAsset;
import com.bloxbean.cardano.yaci.store.assets.storage.AssetStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AssetService {
    private final AssetStorage assetStorage;

    public List<TxAsset> getAssetsByTx(String txHash) {
        return assetStorage.findByTxHash(txHash);
    }
}

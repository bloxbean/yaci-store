package com.bloxbean.cardano.yaci.store.api.assets.service;

import com.bloxbean.cardano.yaci.store.assets.domain.TxAsset;
import com.bloxbean.cardano.yaci.store.assets.storage.AssetStorageReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AssetService {
    private final AssetStorageReader assetReader;

    public List<TxAsset> getAssetsByTx(String txHash) {
        return assetReader.findByTxHash(txHash);
    }

    public List<TxAsset> getAssetTxsByFingerprint(String fingerprint, int page, int count) {
        return assetReader.findByFingerprint(fingerprint, page, count);
    }

    public List<TxAsset> getAssetTxsByPolicyId(String policyId, int page, int count) {
        return assetReader.findByPolicy(policyId, page, count);
    }

    public List<TxAsset> getAssetTxsByUnit(String unit, int page, int count) {
        return assetReader.findByUnit(unit, page, count);
    }

    public Optional<Integer> getSupplyByFingerprint(String fingerprint) {
        return assetReader.getSupplyByFingerprint(fingerprint);
    }

    public Optional<Integer> getSupplyByUnit(String unit) {
        return assetReader.getSupplyByUnit(unit);
    }

    public Optional<Integer> getSupplyByPolicy(String policyId) {
        return assetReader.getSupplyByPolicy(policyId);
    }
}

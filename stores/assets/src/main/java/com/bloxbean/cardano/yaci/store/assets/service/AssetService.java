package com.bloxbean.cardano.yaci.store.assets.service;

import com.bloxbean.cardano.yaci.store.assets.domain.TxAsset;
import com.bloxbean.cardano.yaci.store.assets.storage.AssetStorage;
import io.micrometer.observation.ObservationFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AssetService {
    private final AssetStorage assetStorage;

    public List<TxAsset> getAssetsByTx(String txHash) {
        return assetStorage.findByTxHash(txHash);
    }

    public List<TxAsset> getAssetTxsByFingerprint(String fingerprint, int page, int count) {
        return assetStorage.findByFingerprint(fingerprint, page, count);
    }

    public List<TxAsset> getAssetTxsByPolicyId(String policyId, int page, int count) {
        return assetStorage.findByPolicy(policyId, page, count);
    }

    public List<TxAsset> getAssetTxsByUnit(String unit, int page, int count) {
        return assetStorage.findByUnit(unit, page, count);
    }

    public Optional<Integer> getSupplyByFingerprint(String fingerprint) {
        return assetStorage.getSupplyByFingerprint(fingerprint);
    }

    public Optional<Integer> getSupplyByUnit(String unit) {
        return assetStorage.getSupplyByUnit(unit);
    }

    public Optional<Integer> getSupplyByPolicy(String policyId) {
        return assetStorage.getSupplyByPolicy(policyId);
    }
}

package com.bloxbean.cardano.yaci.store.assets.storage.impl;

import com.bloxbean.cardano.yaci.store.assets.domain.TxAsset;
import com.bloxbean.cardano.yaci.store.assets.storage.AssetStorageReader;
import com.bloxbean.cardano.yaci.store.assets.storage.impl.mapper.AssetMapper;
import com.bloxbean.cardano.yaci.store.assets.storage.impl.model.TxAssetEntity;
import com.bloxbean.cardano.yaci.store.assets.storage.impl.repository.TxAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class AssetStorageReaderImpl implements AssetStorageReader {

    private final TxAssetRepository txAssetRepository;
    private final AssetMapper assetMapper;

    @Override
    public List<TxAsset> findByTxHash(String txHash) {
        List<TxAssetEntity> txAssetEntities = txAssetRepository.findByTxHash(txHash);
        return txAssetEntities.stream().map(assetMapper::toTxAsset).toList();
    }

    @Override
    public List<TxAsset> findByFingerprint(String fingerprint, int page, int count) {
        Pageable sortedHistory =
                PageRequest.of(page, count, Sort.by("slot", "txIndex", "txHash", "unit", "id").descending());

        return txAssetRepository.findByFingerprint(fingerprint, sortedHistory).stream().map(assetMapper::toTxAsset).toList();
    }

    @Override
    public List<TxAsset> findByPolicy(String policyId, int page, int count) {
        Pageable sortedHistory =
                PageRequest.of(page, count, Sort.by("slot", "txIndex", "txHash", "unit", "id").descending());

        return txAssetRepository.findByPolicy(policyId, sortedHistory).stream().map(assetMapper::toTxAsset).toList();
    }

    @Override
    public List<TxAsset> findByUnit(String unit, int page, int count) {
        Pageable sortedHistory =
                PageRequest.of(page, count, Sort.by("slot", "txIndex", "txHash", "unit", "id").descending());

        return txAssetRepository.findByUnit(unit, sortedHistory).stream().map(assetMapper::toTxAsset).toList();
    }

    @Override
    public Optional<BigInteger> getSupplyByFingerprint(String fingerprint) {
        return txAssetRepository.getSupplyByFingerprint(fingerprint);
    }

    @Override
    public Optional<BigInteger> getSupplyByUnit(String unit) {
        return txAssetRepository.getSupplyByUnit(unit);
    }

    @Override
    public Optional<BigInteger> getSupplyByPolicy(String policyId) {
        return txAssetRepository.getSupplyByPolicy(policyId);
    }
}

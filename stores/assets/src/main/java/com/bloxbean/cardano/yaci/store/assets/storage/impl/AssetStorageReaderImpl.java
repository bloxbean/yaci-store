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
        Pageable sortedBySlot =
                PageRequest.of(page, count, Sort.by("slot").descending());

        return txAssetRepository.findByFingerprint(fingerprint, sortedBySlot).stream().map(assetMapper::toTxAsset).toList();
    }

    @Override
    public List<TxAsset> findByPolicy(String policyId, int page, int count) {
        Pageable sortedBySlot =
                PageRequest.of(page, count, Sort.by("slot").descending());

        return txAssetRepository.findByPolicy(policyId, sortedBySlot).stream().map(assetMapper::toTxAsset).toList();
    }

    @Override
    public List<TxAsset> findByUnit(String unit, int page, int count) {
        Pageable sortedBySlot =
                PageRequest.of(page, count, Sort.by("slot").descending());

        return txAssetRepository.findByUnit(unit, sortedBySlot).stream().map(assetMapper::toTxAsset).toList();
    }

    @Override
    public Optional<Integer> getSupplyByFingerprint(String fingerprint) {
        return txAssetRepository.getSupplyByFingerprint(fingerprint);
    }

    @Override
    public Optional<Integer> getSupplyByUnit(String unit) {
        return txAssetRepository.getSupplyByUnit(unit);
    }

    @Override
    public Optional<Integer> getSupplyByPolicy(String policyId) {
        return txAssetRepository.getSupplyByPolicy(policyId);
    }
}

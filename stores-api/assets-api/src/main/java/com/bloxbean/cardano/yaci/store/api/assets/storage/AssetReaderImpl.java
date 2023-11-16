package com.bloxbean.cardano.yaci.store.api.assets.storage;

import com.bloxbean.cardano.yaci.store.api.assets.storage.repository.TxAssetReadRepository;
import com.bloxbean.cardano.yaci.store.assets.domain.TxAsset;
import com.bloxbean.cardano.yaci.store.assets.storage.AssetMapper;
import com.bloxbean.cardano.yaci.store.assets.storage.model.TxAssetEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class AssetReaderImpl implements AssetReader {
    private final TxAssetReadRepository txAssetReadRepository;
    private final AssetMapper assetMapper;

    @Override
    public List<TxAsset> findByTxHash(String txHash) {
        List<TxAssetEntity> txAssetEntities = txAssetReadRepository.findByTxHash(txHash);
        return txAssetEntities.stream().map(assetMapper::toTxAsset).toList();
    }

    @Override
    public List<TxAsset> findByFingerprint(String fingerprint, int page, int count) {
        Pageable sortedBySlot =
                PageRequest.of(page, count, Sort.by("slot").descending());

        return txAssetReadRepository.findByFingerprint(fingerprint, sortedBySlot).stream().map(assetMapper::toTxAsset).toList();
    }

    @Override
    public List<TxAsset> findByPolicy(String policyId, int page, int count) {
        Pageable sortedBySlot =
                PageRequest.of(page, count, Sort.by("slot").descending());

        return txAssetReadRepository.findByPolicy(policyId, sortedBySlot).stream().map(assetMapper::toTxAsset).toList();
    }

    @Override
    public List<TxAsset> findByUnit(String unit, int page, int count) {
        Pageable sortedBySlot =
                PageRequest.of(page, count, Sort.by("slot").descending());

        return txAssetReadRepository.findByUnit(unit, sortedBySlot).stream().map(assetMapper::toTxAsset).toList();
    }

    @Override
    public Optional<Integer> getSupplyByFingerprint(String fingerprint) {
        return txAssetReadRepository.getSupplyByFingerprint(fingerprint);
    }

    @Override
    public Optional<Integer> getSupplyByUnit(String unit) {
        return txAssetReadRepository.getSupplyByUnit(unit);
    }

    @Override
    public Optional<Integer> getSupplyByPolicy(String policyId) {
        return txAssetReadRepository.getSupplyByPolicy(policyId);
    }
}

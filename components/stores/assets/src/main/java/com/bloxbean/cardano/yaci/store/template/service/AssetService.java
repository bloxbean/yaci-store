package com.bloxbean.cardano.yaci.store.template.service;

import com.bloxbean.cardano.yaci.store.template.domain.MintType;
import com.bloxbean.cardano.yaci.store.template.domain.TxAsset;
import com.bloxbean.cardano.yaci.store.template.repository.TxAssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AssetService {
    private final TxAssetRepository txAssetRepository;

    public List<TxAsset> getAssetsByTx(String txHash) {
        return txAssetRepository.findByTxHash(txHash)
                .stream()
                .map(txAssetEntity -> TxAsset.builder()
                        .txHash(txHash)
                        .policy(txAssetEntity.getPolicy())
                        .assetName(txAssetEntity.getAssetName())
                        .unit(txAssetEntity.getUnit())
                        .quantity(txAssetEntity.getQuantity())
                        .mintType(txAssetEntity.getMintType() == MintType.MINT? "Mint": "Burn")
                        .build())
                .collect(Collectors.toList());
    }
}

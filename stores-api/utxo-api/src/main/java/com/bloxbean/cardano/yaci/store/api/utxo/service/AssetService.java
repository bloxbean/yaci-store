package com.bloxbean.cardano.yaci.store.api.utxo.service;

import com.bloxbean.cardano.yaci.store.common.domain.Utxo;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.utxo.domain.AssetTransaction;
import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorageReader;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component("utxoAssetService")
@RequiredArgsConstructor
public class AssetService {

    private final UtxoStorageReader utxoStorage;

    public List<Utxo> getUtxosByAsset(@NonNull String unit, int page, int count, Order order) {
        return utxoStorage.findUtxosByAsset(unit, page, count, order).stream()
                .map(UtxoUtil::addressUtxoToUtxo)
                .collect(Collectors.toList());
    }

    public List<AssetTransaction> getAssetTransactionsByAsset(String unit, int page, int count, Order order) {
        return utxoStorage.findTransactionsByAsset(unit, page, count, order);
    }
}

package com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.repository;

import com.bloxbean.cardano.yaci.store.utxo.domain.AssetHolder;

import java.util.List;

public interface CustomUtxoRepository {
    List<AssetHolder> findUtxosByUnit(String unit);
}

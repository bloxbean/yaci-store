package com.bloxbean.cardano.yaci.store.utxo.storage;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.TxInput;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;

import java.util.List;
import java.util.Optional;

public interface UtxoStorage {

    void saveUnspent(List<AddressUtxo> addressUtxoList);
    void saveSpent(List<TxInput> addressUtxoList);
    Optional<AddressUtxo> findById(String txHash, int outputIndex);
    List<AddressUtxo> findAllByIds(List<UtxoKey> utxoKeys);
    int deleteUnspentBySlotGreaterThan(Long slot);
    int deleteSpentBySlotGreaterThan(Long slot);
    int deleteBySpentAndBlockLessThan(Long block);
}

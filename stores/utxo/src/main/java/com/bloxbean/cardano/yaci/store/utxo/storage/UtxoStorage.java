package com.bloxbean.cardano.yaci.store.utxo.storage;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.TxInput;

import java.util.List;

public interface UtxoStorage {
    void saveUnspent(List<AddressUtxo> addressUtxoList);
    void saveSpent(List<TxInput> addressUtxoList);

    int deleteUnspentBySlotGreaterThan(Long slot);
    int deleteSpentBySlotGreaterThan(Long slot);

}

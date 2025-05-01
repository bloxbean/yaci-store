package com.bloxbean.cardano.yaci.store.ledgerstate.storage;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.TxInput;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.UtxoCache;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.UtxoStorageImpl;
import org.jooq.DSLContext;

import java.util.List;

/**
 * UtxoStorage implementation which doesn't do any write to DB
 */
public class DummyDBUtxoStorage extends UtxoStorageImpl {

    public DummyDBUtxoStorage(DSLContext dsl,
                              UtxoCache utxoCache) {
        super(dsl, utxoCache);
    }

    @Override
    public void saveUnspent(List<AddressUtxo> addressUtxoList) {

    }

    @Override
    public void saveSpent(List<TxInput> addressUtxoList) {

    }

    @Override
    public int deleteUnspentBySlotGreaterThan(Long slot) {
        return 0;
    }

    @Override
    public int deleteSpentBySlotGreaterThan(Long slot) {
        return 0;
    }

}

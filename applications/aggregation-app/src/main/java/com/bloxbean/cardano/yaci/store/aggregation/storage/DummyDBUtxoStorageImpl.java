package com.bloxbean.cardano.yaci.store.aggregation.storage;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.TxInput;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.UtxoCache;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.UtxoStorageImpl;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.repository.TxInputRepository;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.repository.UtxoRepository;
import org.jooq.DSLContext;

import java.util.List;

/**
 * UtxoStorage implementation which doesn't do any write to DB
 */
public class DummyDBUtxoStorageImpl extends UtxoStorageImpl {

    public DummyDBUtxoStorageImpl(UtxoRepository utxoRepository, TxInputRepository spentOutputRepository, DSLContext dsl, UtxoCache utxoCache) {
        super(utxoRepository, spentOutputRepository, dsl, utxoCache);
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

    @Override
    public int deleteBySpentAndBlockLessThan(Long block) {
        return 0;
    }
}

package com.bloxbean.cardano.yaci.store.aggregation.storage;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.TxInput;
import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoCache;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.JpaUtxoStorage;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.repository.JpaTxInputRepository;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.repository.JpaUtxoRepository;
import org.jooq.DSLContext;

import java.util.List;

/**
 * UtxoStorage implementation which doesn't do any write to DB
 */
public class DummyDBJpaUtxoStorage extends JpaUtxoStorage {

    public DummyDBJpaUtxoStorage(JpaUtxoRepository jpaUtxoRepository, JpaTxInputRepository spentOutputRepository, DSLContext dsl, UtxoCache utxoCache) {
        super(jpaUtxoRepository, spentOutputRepository, dsl, utxoCache);
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

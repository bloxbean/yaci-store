package com.bloxbean.cardano.yaci.store.ledgerstate.storage;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.TxInput;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.UtxoCache;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.UtxoStorageImpl;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.repository.TxInputRepository;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.repository.UtxoRepository;
import org.jooq.DSLContext;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

/**
 * UtxoStorage implementation which doesn't do any write to DB
 */
public class DummyDBUtxoStorage extends UtxoStorageImpl {

    public DummyDBUtxoStorage(UtxoRepository utxoRepository,
                              TxInputRepository spentOutputRepository,
                              DSLContext dsl,
                              UtxoCache utxoCache,
                              PlatformTransactionManager transactionManager) {
        super(utxoRepository, spentOutputRepository, dsl, utxoCache, transactionManager);
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

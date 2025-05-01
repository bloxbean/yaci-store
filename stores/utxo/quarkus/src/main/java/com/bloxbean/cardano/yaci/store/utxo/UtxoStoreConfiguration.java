package com.bloxbean.cardano.yaci.store.utxo;

import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.utxo.storage.AddressStorage;
import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorage;
import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorageReader;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.AddressStorageImpl;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.UtxoCache;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.UtxoStorageImpl;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.UtxoStorageReaderImpl;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.jooq.DSLContext;

@ApplicationScoped
public class UtxoStoreConfiguration {
    @Produces
    public UtxoStorage utxoStorage(DSLContext dslContext,
                                   UtxoCache utxoCache) {
        return new UtxoStorageImpl(dslContext, utxoCache);
    }

    @Produces
    public UtxoStorageReader utxoStorageReader(DSLContext dslContext) {
        return new UtxoStorageReaderImpl(dslContext);
    }

    @Produces
    public AddressStorage addressStorage(DSLContext dslContext, StoreProperties storeProperties) {
        return new AddressStorageImpl(dslContext, storeProperties);
    }
}

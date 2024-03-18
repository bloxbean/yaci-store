package com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.config;

import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorage;
import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorageReader;
import com.bloxbean.cardano.yaci.store.utxo.storage.config.StorageConfig;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.JpaUtxoStorage;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.JpaUtxoStorageReader;
import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoCache;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.repository.JpaTxInputRepository;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.repository.JpaUtxoRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;

@Getter
@RequiredArgsConstructor
public class JpaConfig implements StorageConfig {

    private final JpaUtxoRepository jpaUtxoRepository;
    private final JpaTxInputRepository spentOutputRepository;
    private final DSLContext dslContext;
    private final UtxoCache utxoCache;

    @Override
    public UtxoStorage utxoStorage() {
        return new JpaUtxoStorage(jpaUtxoRepository, spentOutputRepository, dslContext, utxoCache);
    }

    @Override
    public UtxoStorageReader utxoStorageReader() {
        return new JpaUtxoStorageReader(jpaUtxoRepository, spentOutputRepository, dslContext);
    }
}

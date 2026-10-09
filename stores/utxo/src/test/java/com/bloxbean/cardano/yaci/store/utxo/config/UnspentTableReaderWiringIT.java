package com.bloxbean.cardano.yaci.store.utxo.config;

import com.bloxbean.cardano.yaci.store.utxo.UtxoStoreConfiguration;
import com.bloxbean.cardano.yaci.store.utxo.UtxoStoreProperties;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.UnspentTableUtxoStorageReader;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.UtxoStorageReaderImpl;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.repository.UtxoRepository;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/** unspentTableReadEnabled decides which reader the store's API is given. */
@SpringBootTest
class UnspentTableReaderWiringIT {

    @Autowired
    UtxoRepository utxoRepository;

    @Autowired
    DSLContext dsl;

    private Object readerWith(boolean readEnabled) {
        UtxoStoreProperties properties = UtxoStoreProperties.builder().unspentTableReadEnabled(readEnabled).build();
        return new UtxoStoreConfiguration().utxoStorageReader(utxoRepository, dsl, properties);
    }

    @Test
    void readsTheAntiJoinByDefault() {
        assertThat(readerWith(false)).isInstanceOf(UtxoStorageReaderImpl.class);
    }

    @Test
    void readsTheUnspentTableWhenEnabled() {
        assertThat(readerWith(true)).isInstanceOf(UnspentTableUtxoStorageReader.class);
    }
}

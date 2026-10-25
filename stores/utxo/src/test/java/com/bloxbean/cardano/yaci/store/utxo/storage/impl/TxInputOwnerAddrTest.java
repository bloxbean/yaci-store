package com.bloxbean.cardano.yaci.store.utxo.storage.impl;

import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import com.bloxbean.cardano.yaci.store.common.service.CursorService;
import com.bloxbean.cardano.yaci.store.common.service.StoreStateService;
import com.bloxbean.cardano.yaci.store.events.internal.PreBlockSyncEvent;
import com.bloxbean.cardano.yaci.store.utxo.UtxoStoreProperties;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/** tx_input.owner_addr is PostgreSQL only: on H2 and MySQL, or with the flag off, nothing is written, read or cached. */
class TxInputOwnerAddrTest {

    private static TxInputOwnerAddr ownerAddr(UtxoStoreProperties properties, SQLDialect dialect, StoreStateService stateService) {
        return new TxInputOwnerAddr(properties, DSL.using(dialect), mock(PlatformTransactionManager.class), stateService,
                mock(CursorService.class), () -> {
            throw new AssertionError("no cache should be made");
        });
    }

    @Test
    void onH2AndMySqlNothingIsWrittenOrRead() {
        for (SQLDialect dialect : new SQLDialect[]{SQLDialect.H2, SQLDialect.MYSQL}) {
            StoreStateService stateService = mock(StoreStateService.class);
            TxInputOwnerAddr ownerAddr = ownerAddr(UtxoStoreProperties.builder().txInputOwnerAddrEnabled(true).build(),
                    dialect, stateService);

            ownerAddr.handlePreBlockSync(new PreBlockSyncEvent(null));

            assertThat(ownerAddr.isLive()).isFalse();
            assertThat(ownerAddr.isReadable()).isFalse();
            verifyNoInteractions(stateService);
        }
    }

    @Test
    void withTheFlagOffNothingIsWrittenOrRead() {
        StoreStateService stateService = mock(StoreStateService.class);
        TxInputOwnerAddr ownerAddr = ownerAddr(UtxoStoreProperties.builder().build(), SQLDialect.POSTGRES, stateService);

        ownerAddr.handlePreBlockSync(new PreBlockSyncEvent(null));

        assertThat(ownerAddr.isLive()).isFalse();
        assertThat(ownerAddr.isReadable()).isFalse();
        verifyNoInteractions(stateService);
    }

    @Test
    void theCacheKeyIsTheTxHashBytesAndTheIndex() {
        UtxoKey utxoKey = new UtxoKey("ab".repeat(32), 65535);

        byte[] bytes = UtxoKeyCodec.INSTANCE.encode(utxoKey);

        assertThat(bytes).hasSize(34);
        assertThat(UtxoKeyCodec.INSTANCE.decode(bytes)).isEqualTo(utxoKey);
    }
}

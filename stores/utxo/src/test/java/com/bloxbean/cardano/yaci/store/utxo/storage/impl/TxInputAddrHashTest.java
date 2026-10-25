package com.bloxbean.cardano.yaci.store.utxo.storage.impl;

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

/** tx_input.addr_hash is PostgreSQL only: on H2 and MySQL the flag changes nothing. */
class TxInputAddrHashTest {

    @Test
    void onH2AndMySqlNothingIsWrittenOrRead() {
        for (SQLDialect dialect : new SQLDialect[]{SQLDialect.H2, SQLDialect.MYSQL}) {
            StoreStateService stateService = mock(StoreStateService.class);
            TxInputAddrHash addrHash = new TxInputAddrHash(UtxoStoreProperties.builder().txInputAddrHashEnabled(true).build(),
                    DSL.using(dialect), mock(PlatformTransactionManager.class), stateService, mock(CursorService.class));

            addrHash.handlePreBlockSync(new PreBlockSyncEvent(null));

            assertThat(addrHash.isLive()).isFalse();
            assertThat(addrHash.isReadable()).isFalse();
            verifyNoInteractions(stateService);
        }
    }

    @Test
    void withTheFlagOffNothingIsWrittenOrRead() {
        StoreStateService stateService = mock(StoreStateService.class);
        TxInputAddrHash addrHash = new TxInputAddrHash(UtxoStoreProperties.builder().build(),
                DSL.using(SQLDialect.POSTGRES), mock(PlatformTransactionManager.class), stateService, mock(CursorService.class));

        addrHash.handlePreBlockSync(new PreBlockSyncEvent(null));

        assertThat(addrHash.isLive()).isFalse();
        assertThat(addrHash.isReadable()).isFalse();
        verifyNoInteractions(stateService);
    }
}

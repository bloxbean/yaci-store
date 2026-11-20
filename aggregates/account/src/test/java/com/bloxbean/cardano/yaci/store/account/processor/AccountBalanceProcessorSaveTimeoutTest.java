package com.bloxbean.cardano.yaci.store.account.processor;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.account.AccountStoreProperties;
import com.bloxbean.cardano.yaci.store.account.service.AccountConfigService;
import com.bloxbean.cardano.yaci.store.account.service.AddressBalanceSnapshotService;
import com.bloxbean.cardano.yaci.store.account.service.BalanceSnapshotService;
import com.bloxbean.cardano.yaci.store.account.service.MetricCollectorService;
import com.bloxbean.cardano.yaci.store.account.storage.AccountBalanceStorage;
import com.bloxbean.cardano.yaci.store.client.staking.StakingClient;
import com.bloxbean.cardano.yaci.store.client.utxo.UtxoClient;
import com.bloxbean.cardano.yaci.store.common.executor.DbWorkTimeoutException;
import com.bloxbean.cardano.yaci.store.common.executor.ParallelExecutor;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.internal.PreCommitEvent;
import com.bloxbean.cardano.yaci.store.utxo.domain.AddressUtxoEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A stake balance save that never returns (on mainnet: a virtual thread wedged inside the PostgreSQL driver, its
 * transaction left open with row locks held) must fail the batch instead of holding the sync thread forever, and must
 * end the stuck transaction so later sync sessions are not queued behind its locks.
 */
class AccountBalanceProcessorSaveTimeoutTest {
    private final CountDownLatch release = new CountDownLatch(1);
    private final CountDownLatch saveStarted = new CountDownLatch(1);

    private AccountBalanceStorage accountBalanceStorage;
    private AccountConfigService accountConfigService;
    private Connection connection;
    private AccountBalanceProcessor processor;

    @BeforeEach
    void setUp() throws Exception {
        accountBalanceStorage = mock(AccountBalanceStorage.class);
        accountConfigService = mock(AccountConfigService.class);

        // A real transaction manager over a mocked connection, so the save runs in a Spring transaction like in the app
        connection = mock(Connection.class);
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenReturn(connection);

        AccountStoreProperties properties = AccountStoreProperties.builder()
                .balanceAggregationEnabled(true)
                .addressBalanceEnabled(false)
                .stakeAddressBalanceEnabled(true)
                .balanceSaveTimeoutSeconds(1)
                .build();

        doAnswer(invocation -> {
            saveStarted.countDown();
            release.await(60, TimeUnit.SECONDS); // stuck, like the driver call on mainnet
            return null;
        }).when(accountBalanceStorage).saveStakeAddressBalances(anyList());

        processor = new AccountBalanceProcessor(accountBalanceStorage, mock(AccountBalanceHistoryCleanupHelper.class),
                properties, mock(UtxoClient.class), mock(StakingClient.class), accountConfigService,
                mock(BalanceSnapshotService.class), mock(AddressBalanceSnapshotService.class), new ParallelExecutor(),
                new DataSourceTransactionManager(dataSource), mock(MetricCollectorService.class), mock(EraService.class),
                dataSource);
    }

    @AfterEach
    void tearDown() {
        release.countDown();
    }

    @Test
    void stuckStakeBalanceSave_failsTheBatch_andAbortsItsConnection() throws Exception {
        EventMetadata metadata = EventMetadata.builder()
                .block(1) // the first block after an empty account_config, so no block gap
                .slot(1000)
                .era(Era.Babbage)
                .build();
        processor.handleAddressUtxoEvent(new AddressUtxoEvent(metadata, List.of()));

        assertTimeoutPreemptively(Duration.ofSeconds(20), () ->
                assertThatThrownBy(() -> processor.handlePostProcessingEvent(new PreCommitEvent(metadata)))
                        .isInstanceOf(DbWorkTimeoutException.class)
                        .hasMessageContaining("Account balance save up to block 1"));

        verify(accountBalanceStorage, timeout(5000)).saveStakeAddressBalances(anyList());
        // The stuck save's connection is aborted, so the database rolls its transaction back and frees its locks
        verify(connection).abort(any(Executor.class));
        verify(connection, never()).commit();
        // The batch is not recorded as processed
        verify(accountConfigService, never()).upateConfig(anyString(), any(), anyLong(), any(), anyLong());
    }

    @Test
    void stakeBalanceSaveThatFinishes_isNotAborted() throws Exception {
        release.countDown();
        EventMetadata metadata = EventMetadata.builder()
                .block(1) // the first block after an empty account_config, so no block gap
                .slot(1000)
                .era(Era.Babbage)
                .build();
        processor.handleAddressUtxoEvent(new AddressUtxoEvent(metadata, List.of()));

        assertTimeoutPreemptively(Duration.ofSeconds(20), () ->
                processor.handlePostProcessingEvent(new PreCommitEvent(metadata)));

        verify(connection).commit();
        verify(connection, never()).abort(any(Executor.class));
        verify(accountConfigService).upateConfig(anyString(), eq(null), eq(1L), any(), eq(1000L));
    }
}

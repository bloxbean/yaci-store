package com.bloxbean.cardano.yaci.store.analytics.config;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.account.AccountStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.exporter.TableExporterRegistry;
import com.bloxbean.cardano.yaci.store.analytics.gap.GapDetectionService;
import com.bloxbean.cardano.yaci.store.blocks.BlocksStoreProperties;
import com.bloxbean.cardano.yaci.store.core.configuration.GenesisConfig;
import com.bloxbean.cardano.yaci.store.transaction.TransactionStoreProperties;
import com.bloxbean.cardano.yaci.store.utxo.UtxoStoreProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.env.MockEnvironment;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PruningSafetyCheckTest {

    private final AnalyticsStoreProperties properties = new AnalyticsStoreProperties();
    private final MockEnvironment environment = new MockEnvironment();
    private final UtxoStoreProperties utxo = new UtxoStoreProperties();
    private final TransactionStoreProperties transaction = new TransactionStoreProperties();
    private final BlocksStoreProperties blocks = new BlocksStoreProperties();
    private final AccountStoreProperties account = new AccountStoreProperties();
    private TableExporterRegistry registry;
    private PruningSafetyCheck check;

    @BeforeEach
    void setUp() {
        registry = mock(TableExporterRegistry.class);
        when(registry.hasExporter(anyString())).thenReturn(true);
        when(registry.isEnabled(anyString())).thenReturn(true);

        GapDetectionService gapDetection = mock(GapDetectionService.class);
        when(gapDetection.getFinalityWindow()).thenReturn(Duration.ofHours(13)); // mainnet: 12 h + 1 h margin
        GenesisConfig genesis = mock(GenesisConfig.class);
        when(genesis.slotDuration(Era.Shelley)).thenReturn(1.0);
        when(genesis.getActiveSlotsCoeff()).thenReturn(0.05);

        check = new PruningSafetyCheck(properties, registry, gapDetection, genesis, environment,
                provider(utxo), provider(transaction), provider(blocks), provider(account));
    }

    @Test
    void aShortUtxoWindowIsRaisedToTheExportDelayPlusTheMargin() {
        utxo.setPruningEnabled(true); // pruning-safe-blocks=2160: ~12 h

        check.afterSingletonsInstantiated();
        check.setHeld(false);

        // 37 h export delay + 12 h margin = 49 h = 8820 blocks of 20 s
        assertThat(utxo.getPruningSafeBlocks()).isEqualTo(8820);
    }

    @Test
    void pruningIsHeldFromStartupUntilTheExportHasCaughtUp() {
        utxo.setPruningEnabled(true);
        account.setHistoryCleanupEnabled(true);
        environment.setProperty("store.account.enabled", "true");

        check.afterSingletonsInstantiated();
        assertThat(check.isHeld()).isTrue();
        assertThat(utxo.getPruningSafeBlocks()).isEqualTo((int) PruningSafetyCheck.HOLD_WINDOW);
        assertThat(account.getBalanceCleanupSlotCount()).isEqualTo(PruningSafetyCheck.HOLD_WINDOW);

        check.setHeld(false);
        assertThat(utxo.getPruningSafeBlocks()).isEqualTo(8820);
        assertThat(account.getBalanceCleanupSlotCount()).isEqualTo(176_400);

        check.setHeld(true);   // behind again, e.g. a day waiting for its retry
        assertThat(utxo.getPruningSafeBlocks()).isEqualTo((int) PruningSafetyCheck.HOLD_WINDOW);
    }

    @Test
    void aWindowThatIsAlreadyLongEnoughIsKeptButStillHeldWhileBehind() {
        utxo.setPruningEnabled(true);
        utxo.setPruningSafeBlocks(20_000);

        check.afterSingletonsInstantiated();
        check.setHeld(false);
        assertThat(utxo.getPruningSafeBlocks()).isEqualTo(20_000);
        check.setHeld(true);
        assertThat(utxo.getPruningSafeBlocks()).isEqualTo((int) PruningSafetyCheck.HOLD_WINDOW);
    }

    @Test
    void pruningOfTablesThatAreNotExportedIsLeftAlone() {
        utxo.setPruningEnabled(true);
        when(registry.isEnabled(anyString())).thenReturn(false);

        check.afterSingletonsInstantiated();
        check.setHeld(true);
        assertThat(utxo.getPruningSafeBlocks()).isEqualTo(2160);
        assertThat(check.isHeld()).isFalse();
    }

    @Test
    void everyPruningFeatureIsCheckedAgainstTheTablesItDeletesFrom() {
        transaction.setPruningEnabled(true);
        blocks.setCborPruningEnabled(true);
        account.setHistoryCleanupEnabled(true);
        environment.setProperty("store.account.enabled", "true");

        assertThat(check.check()).extracting(PruningSafetyCheck.Violation::table).containsExactlyInAnyOrder(
                "transaction", "transaction_witness", "transaction_cbor", "block_cbor",
                "address_balance", "stake_address_balance", "stake_address_balance_snapshot");
        assertThat(check.check()).allSatisfy(v -> assertThat(v.minimumWindow()).isEqualTo(176_400)); // 49 h of slots
    }

    @Test
    void aLongEnoughWindowPasses() {
        transaction.setPruningEnabled(true);
        transaction.setPruningSafeSlots(176_400);

        assertThat(check.check()).isEmpty();
    }

    @Test
    void disabledExportersAndDisabledStoresAreNotChecked() {
        utxo.setPruningEnabled(true);
        account.setPruningEnabled(true); // store.account.enabled is false by default
        when(registry.isEnabled("address_utxo")).thenReturn(false);

        assertThat(check.check()).extracting(PruningSafetyCheck.Violation::table).containsExactly("tx_input");

        environment.setProperty("store.utxo.enabled", "false");
        assertThat(check.check()).isEmpty();
    }

    @Test
    void operatorsCanKeepTheirWindowAndAcceptTheLoss() {
        utxo.setPruningEnabled(true);
        properties.getPruningCheck().setAcceptDataLoss(true);

        assertThatCode(check::afterSingletonsInstantiated).doesNotThrowAnyException();
        check.setHeld(true);
        assertThat(utxo.getPruningSafeBlocks()).isEqualTo(2160);
        assertThat(check.isHeld()).isFalse();
    }

    @Test
    void moreBufferDaysNeedALongerWindow() {
        properties.getContinuousSync().setBufferDays(2);
        properties.getPruningCheck().setMarginHours(0);

        assertThat(check.requiredRetention()).isEqualTo(Duration.ofHours(13 + 48));
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> provider(T bean) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(bean);
        return provider;
    }
}

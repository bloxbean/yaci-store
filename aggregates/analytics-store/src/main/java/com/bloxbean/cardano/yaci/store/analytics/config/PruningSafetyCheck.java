package com.bloxbean.cardano.yaci.store.analytics.config;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.account.AccountStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.exporter.TableExporterRegistry;
import com.bloxbean.cardano.yaci.store.analytics.gap.GapDetectionService;
import com.bloxbean.cardano.yaci.store.blocks.BlocksStoreProperties;
import com.bloxbean.cardano.yaci.store.core.configuration.GenesisConfig;
import com.bloxbean.cardano.yaci.store.transaction.TransactionStoreProperties;
import com.bloxbean.cardano.yaci.store.utxo.UtxoStoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Refuses to start the analytics store when a PostgreSQL pruning setting deletes rows of an
 * enabled exporter's source table before the analytics export has read them.
 *
 * <p>A daily partition is exported once its oldest rows are {@code buffer-days × 24 h +}
 * the finality window old (37 h on mainnet with the defaults). The pruning features delete
 * rows that are older than their window relative to the sync tip (about 12 h by default), so
 * with the defaults most of each day would be deleted before it is exported, silently.</p>
 *
 * <p>Checked features (each only when its store is enabled and its pruning is switched on):</p>
 * <ul>
 *   <li>UTxO pruning ({@code store.utxo.pruning-safe-blocks}): {@code address_utxo}, {@code tx_input}</li>
 *   <li>Transaction pruning ({@code store.transaction.pruning-safe-slots}): {@code transaction},
 *       {@code transaction_witness}, {@code transaction_cbor}</li>
 *   <li>Transaction CBOR pruning ({@code store.transaction.cbor-pruning-safe-slots}): {@code transaction_cbor}</li>
 *   <li>Block CBOR pruning ({@code store.blocks.cbor-pruning-safe-slots}): {@code block_cbor}</li>
 *   <li>Account balance history cleanup and balance pruning ({@code store.account.balance-cleanup-slot-count}):
 *       {@code address_balance}, {@code stake_address_balance}, {@code stake_address_balance_snapshot}</li>
 * </ul>
 *
 * <p>Reward and epoch-stake pruning are not checked: both are anchored on completed AdaPot jobs,
 * and those tables are exported as soon as the same jobs complete.</p>
 *
 * <p>{@code yaci.store.analytics.pruning-check.accept-data-loss=true} turns the refusal into a
 * warning.</p>
 */
@Component
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class PruningSafetyCheck implements SmartInitializingSingleton {

    /** How a pruning window is measured. */
    enum Unit { SLOTS, BLOCKS }

    /**
     * An active pruning feature.
     *
     * @param feature        human-readable name
     * @param enableProperty property that switched it on
     * @param windowProperty property holding its window
     * @param window         the window, in {@code unit}
     * @param unit           slots or blocks
     * @param tables         analytics tables that read the rows it deletes
     */
    record Pruning(String feature, String enableProperty, String windowProperty, long window, Unit unit,
                   List<String> tables) {
    }

    /** An enabled exporter whose source rows are pruned too early. */
    record Violation(String table, Pruning pruning, Duration pruneAfter, Duration required, long minimumWindow) {
        String message() {
            return String.format("Analytics table '%s' is exported from rows that %s (%s=true) deletes once they are "
                            + "%s=%d %s (~%.1f h) old, but a day's oldest rows are exported only when ~%.1f h old. "
                            + "Set %s to at least %d, disable the exporter (yaci.store.analytics.exporter.%s.enabled=false), "
                            + "or set yaci.store.analytics.pruning-check.accept-data-loss=true to accept the missing rows",
                    table, pruning.feature(), pruning.enableProperty(), pruning.windowProperty(), pruning.window(),
                    pruning.unit().name().toLowerCase(), hours(pruneAfter), hours(required),
                    pruning.windowProperty(), minimumWindow, table);
        }
    }

    private final AnalyticsStoreProperties properties;
    private final TableExporterRegistry registry;
    private final GapDetectionService gapDetectionService;
    private final GenesisConfig genesisConfig;
    private final Environment environment;
    private final ObjectProvider<UtxoStoreProperties> utxoProperties;
    private final ObjectProvider<TransactionStoreProperties> transactionProperties;
    private final ObjectProvider<BlocksStoreProperties> blocksProperties;
    private final ObjectProvider<AccountStoreProperties> accountProperties;

    public PruningSafetyCheck(AnalyticsStoreProperties properties,
                              TableExporterRegistry registry,
                              GapDetectionService gapDetectionService,
                              GenesisConfig genesisConfig,
                              Environment environment,
                              ObjectProvider<UtxoStoreProperties> utxoProperties,
                              ObjectProvider<TransactionStoreProperties> transactionProperties,
                              ObjectProvider<BlocksStoreProperties> blocksProperties,
                              ObjectProvider<AccountStoreProperties> accountProperties) {
        this.properties = properties;
        this.registry = registry;
        this.gapDetectionService = gapDetectionService;
        this.genesisConfig = genesisConfig;
        this.environment = environment;
        this.utxoProperties = utxoProperties;
        this.transactionProperties = transactionProperties;
        this.blocksProperties = blocksProperties;
        this.accountProperties = accountProperties;
    }

    /** Runs once all beans exist, so built-in and custom exporters are registered. */
    @Override
    public void afterSingletonsInstantiated() {
        List<Violation> violations = check();
        if (violations.isEmpty()) {
            return;
        }
        if (properties.getPruningCheck().isAcceptDataLoss()) {
            violations.forEach(v -> log.warn("{} (accepted: pruning-check.accept-data-loss=true)", v.message()));
            return;
        }
        StringBuilder message = new StringBuilder("Pruning deletes PostgreSQL rows before the analytics store "
                + "exports them; refusing to start:");
        violations.forEach(v -> message.append(System.lineSeparator()).append(" - ").append(v.message()));
        throw new IllegalStateException(message.toString());
    }

    /** The enabled exporters whose source rows are pruned before they are exported. */
    List<Violation> check() {
        Duration required = requiredRetention();
        List<Violation> violations = new ArrayList<>();
        for (Pruning pruning : activePrunings()) {
            Duration pruneAfter = duration(pruning.window(), pruning.unit());
            if (pruneAfter.compareTo(required) >= 0) {
                continue;
            }
            for (String table : pruning.tables()) {
                if (registry.hasExporter(table) && registry.isEnabled(table)) {
                    violations.add(new Violation(table, pruning, pruneAfter, required,
                            minimumWindow(required, pruning.unit())));
                }
            }
        }
        return violations;
    }

    /**
     * Age of a day's oldest rows when the day is exported (buffer days plus the finality
     * window, see {@link GapDetectionService#getExportEndDate()}), plus the configured margin.
     */
    Duration requiredRetention() {
        int bufferDays = Math.max(1, properties.getContinuousSync().getBufferDays());
        return gapDetectionService.getFinalityWindow()
                .plusDays(bufferDays)
                .plusHours(Math.max(0, properties.getPruningCheck().getMarginHours()));
    }

    List<Pruning> activePrunings() {
        List<Pruning> prunings = new ArrayList<>();

        UtxoStoreProperties utxo = utxoProperties.getIfAvailable();
        if (utxo != null && storeEnabled("utxo", true) && utxo.isPruningEnabled()) {
            prunings.add(new Pruning("UTxO pruning", "store.utxo.pruning-enabled", "store.utxo.pruning-safe-blocks",
                    utxo.getPruningSafeBlocks(), Unit.BLOCKS, List.of("address_utxo", "tx_input")));
        }

        TransactionStoreProperties transaction = transactionProperties.getIfAvailable();
        if (transaction != null && storeEnabled("transaction", true)) {
            if (transaction.isPruningEnabled()) {
                prunings.add(new Pruning("transaction pruning", "store.transaction.pruning-enabled",
                        "store.transaction.pruning-safe-slots", transaction.getPruningSafeSlots(), Unit.SLOTS,
                        List.of("transaction", "transaction_witness", "transaction_cbor")));
            }
            if (transaction.isCborPruningEnabled()) {
                prunings.add(new Pruning("transaction CBOR pruning", "store.transaction.cbor-pruning-enabled",
                        "store.transaction.cbor-pruning-safe-slots", transaction.getCborPruningSafeSlots(), Unit.SLOTS,
                        List.of("transaction_cbor")));
            }
        }

        BlocksStoreProperties blocks = blocksProperties.getIfAvailable();
        if (blocks != null && storeEnabled("blocks", true) && blocks.isCborPruningEnabled()) {
            prunings.add(new Pruning("block CBOR pruning", "store.blocks.cbor-pruning-enabled",
                    "store.blocks.cbor-pruning-safe-slots", blocks.getCborPruningSafeSlots(), Unit.SLOTS,
                    List.of("block_cbor")));
        }

        AccountStoreProperties account = accountProperties.getIfAvailable();
        if (account != null && storeEnabled("account", false)) {
            List<String> balanceTables = List.of("address_balance", "stake_address_balance",
                    "stake_address_balance_snapshot");
            if (account.isHistoryCleanupEnabled()) {
                prunings.add(new Pruning("account balance history cleanup", "store.account.history-cleanup-enabled",
                        "store.account.balance-cleanup-slot-count", account.getBalanceCleanupSlotCount(), Unit.SLOTS,
                        balanceTables));
            } else if (account.isPruningEnabled()) {
                prunings.add(new Pruning("account balance pruning", "store.account.pruning-enabled",
                        "store.account.balance-cleanup-slot-count", account.getBalanceCleanupSlotCount(), Unit.SLOTS,
                        balanceTables));
            }
        }
        return prunings;
    }

    /** Whether {@code store.<name>.enabled} is on; the pruning services exist only then. */
    private boolean storeEnabled(String store, boolean defaultValue) {
        return environment.getProperty("store." + store + ".enabled", Boolean.class, defaultValue);
    }

    private Duration duration(long window, Unit unit) {
        return Duration.ofMillis(Math.round(window * secondsPer(unit) * 1000));
    }

    private long minimumWindow(Duration required, Unit unit) {
        return (long) Math.ceil(required.toMillis() / 1000.0 / secondsPer(unit));
    }

    /** Seconds per slot, or expected seconds per block (slot length / active slot coefficient). */
    private double secondsPer(Unit unit) {
        double slotLength = genesisConfig.slotDuration(Era.Shelley);
        if (slotLength <= 0) {
            slotLength = 1;
        }
        if (unit == Unit.SLOTS) {
            return slotLength;
        }
        double f = genesisConfig.getActiveSlotsCoeff();
        return slotLength / (f > 0 && f <= 1 ? f : 0.05);
    }

    private static double hours(Duration duration) {
        return duration.toMillis() / 3_600_000.0;
    }
}

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
 * Keeps PostgreSQL pruning from deleting rows the analytics store has not exported yet.
 *
 * <p>A daily partition is exported once its oldest rows are {@code buffer-days × 24 h +}
 * the finality window old (37 h on mainnet with the defaults). The pruning features delete
 * rows older than their window relative to the sync tip (about 12 h by default), so without
 * this guard most of each day would be deleted before it is exported, silently.</p>
 *
 * <p>For each active pruning feature that deletes rows of an enabled exporter:</p>
 * <ul>
 *   <li>At startup its window is raised, when it is shorter, to the export delay plus
 *       {@code pruning-check.margin-hours} (49 h on mainnet). The change is logged at WARN.</li>
 *   <li>While the analytics export is behind (the indexer is still syncing with
 *       {@code export-after-sync=true}, or a day that is ready to export has not been exported
 *       yet, for example while a failed export waits for its retry), pruning is held: its window
 *       is set so large that nothing qualifies, because rows of days not exported yet would
 *       otherwise be deleted whatever the window. It is released when the export has caught up.
 *       The pruning services read their window on every run, so this takes effect at once.</li>
 * </ul>
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
 * <p>{@code yaci.store.analytics.pruning-check.accept-data-loss=true} leaves the configured
 * windows alone (never raised, never held) and only warns.</p>
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
                   List<String> tables, java.util.function.LongConsumer setWindow) {
    }

    /**
     * Window that makes a pruning feature delete nothing: the pruning services delete only when
     * {@code tip - window > 0}. It fits the {@code int} window properties.
     */
    static final long HOLD_WINDOW = Integer.MAX_VALUE;

    /** A pruning feature this guard manages, with the window it runs with when not held. */
    record Guarded(Pruning pruning, long window) {
    }

    private final List<Guarded> guarded = new java.util.concurrent.CopyOnWriteArrayList<>();
    /** Null until the first decision; then whether pruning is held. */
    private volatile Boolean held;

    /** An enabled exporter whose source rows are pruned too early. */
    record Violation(String table, Pruning pruning, Duration pruneAfter, Duration required, long minimumWindow) {
        String message() {
            return String.format("Analytics table '%s' is exported from rows that %s (%s=true) deletes once they are "
                            + "%s=%d %s (~%.1f h) old, but a day's oldest rows are exported only when ~%.1f h old",
                    table, pruning.feature(), pruning.enableProperty(), pruning.windowProperty(), pruning.window(),
                    pruning.unit().name().toLowerCase(), hours(pruneAfter), hours(required));
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

    /**
     * Runs once all beans exist (built-in and custom exporters are registered), before the
     * scheduled pruning jobs start: raises windows that are too short, then holds pruning until
     * the first analytics sync run says the export is not behind.
     */
    @Override
    public void afterSingletonsInstantiated() {
        if (properties.getPruningCheck().isAcceptDataLoss()) {
            check().forEach(v -> log.warn("{}; the window is kept (pruning-check.accept-data-loss=true), so these "
                    + "rows are lost to the analytics store", v.message()));
            return;
        }
        Duration required = requiredRetention();
        for (Pruning pruning : activePrunings()) {
            List<String> exported = pruning.tables().stream()
                    .filter(t -> registry.hasExporter(t) && registry.isEnabled(t)).toList();
            if (exported.isEmpty()) {
                continue;
            }
            long minimum = minimumWindow(required, pruning.unit());
            long window = Math.max(pruning.window(), minimum);
            if (window > pruning.window()) {
                pruning.setWindow().accept(window);
                log.warn("{} deletes rows of analytics tables {} after {}={} {} (~{} h), before they are exported "
                                + "(~{} h). Raised {} to {} while the analytics store is enabled. Set it to at least {} to "
                                + "silence this, or pruning-check.accept-data-loss=true to keep the configured window",
                        pruning.feature(), exported, pruning.windowProperty(), pruning.window(),
                        pruning.unit().name().toLowerCase(), String.format("%.1f", hours(duration(pruning.window(), pruning.unit()))),
                        String.format("%.1f", hours(required)), pruning.windowProperty(), window, minimum);
            }
            guarded.add(new Guarded(pruning, window));
        }
        if (!guarded.isEmpty()) {
            setHeld(true);
        }
    }

    /**
     * Hold pruning (nothing is deleted) while the analytics export is behind, release it once it
     * has caught up. Called by the continuous-sync scheduler after each check; a no-op when no
     * pruning feature affects an exported table.
     *
     * @param behind true while the indexer is still syncing with exports deferred, or a day that is
     *               ready to export has not been exported yet
     */
    public void setHeld(boolean behind) {
        if (guarded.isEmpty() || Boolean.valueOf(behind).equals(held)) {
            return;
        }
        for (Guarded g : guarded) {
            g.pruning().setWindow().accept(behind ? HOLD_WINDOW : g.window());
        }
        held = behind;
        List<String> features = guarded.stream().map(g -> g.pruning().feature()).toList();
        if (behind) {
            log.info("Holding {} until the analytics export has caught up, so no row is deleted before it is exported",
                    features);
        } else {
            log.info("Analytics export has caught up; {} runs again with its window", features);
        }
    }

    /** Whether pruning is held now (tests and status). */
    public boolean isHeld() {
        return Boolean.TRUE.equals(held);
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
                    utxo.getPruningSafeBlocks(), Unit.BLOCKS, List.of("address_utxo", "tx_input"),
                    w -> utxo.setPruningSafeBlocks(toInt(w))));
        }

        TransactionStoreProperties transaction = transactionProperties.getIfAvailable();
        if (transaction != null && storeEnabled("transaction", true)) {
            if (transaction.isPruningEnabled()) {
                prunings.add(new Pruning("transaction pruning", "store.transaction.pruning-enabled",
                        "store.transaction.pruning-safe-slots", transaction.getPruningSafeSlots(), Unit.SLOTS,
                        List.of("transaction", "transaction_witness", "transaction_cbor"),
                        w -> transaction.setPruningSafeSlots(toInt(w))));
            }
            if (transaction.isCborPruningEnabled()) {
                prunings.add(new Pruning("transaction CBOR pruning", "store.transaction.cbor-pruning-enabled",
                        "store.transaction.cbor-pruning-safe-slots", transaction.getCborPruningSafeSlots(), Unit.SLOTS,
                        List.of("transaction_cbor"), w -> transaction.setCborPruningSafeSlots(toInt(w))));
            }
        }

        BlocksStoreProperties blocks = blocksProperties.getIfAvailable();
        if (blocks != null && storeEnabled("blocks", true) && blocks.isCborPruningEnabled()) {
            prunings.add(new Pruning("block CBOR pruning", "store.blocks.cbor-pruning-enabled",
                    "store.blocks.cbor-pruning-safe-slots", blocks.getCborPruningSafeSlots(), Unit.SLOTS,
                    List.of("block_cbor"), w -> blocks.setCborPruningSafeSlots(toInt(w))));
        }

        AccountStoreProperties account = accountProperties.getIfAvailable();
        if (account != null && storeEnabled("account", false)) {
            List<String> balanceTables = List.of("address_balance", "stake_address_balance",
                    "stake_address_balance_snapshot");
            if (account.isHistoryCleanupEnabled()) {
                prunings.add(new Pruning("account balance history cleanup", "store.account.history-cleanup-enabled",
                        "store.account.balance-cleanup-slot-count", account.getBalanceCleanupSlotCount(), Unit.SLOTS,
                        balanceTables, account::setBalanceCleanupSlotCount));
            } else if (account.isPruningEnabled()) {
                prunings.add(new Pruning("account balance pruning", "store.account.pruning-enabled",
                        "store.account.balance-cleanup-slot-count", account.getBalanceCleanupSlotCount(), Unit.SLOTS,
                        balanceTables, account::setBalanceCleanupSlotCount));
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

    private static int toInt(long window) {
        return (int) Math.min(window, Integer.MAX_VALUE);
    }

    private static double hours(Duration duration) {
        return duration.toMillis() / 3_600_000.0;
    }
}

package com.bloxbean.cardano.yaci.store.analytics.exporter;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import com.bloxbean.cardano.yaci.store.adapot.job.storage.AdaPotJobStorage;
import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateService;
import com.bloxbean.cardano.yaci.store.analytics.writer.StorageWriter;
import com.bloxbean.cardano.yaci.store.blocks.storage.BlockStorageReader;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Exporter for epoch summary statistics.
 *
 * Partitioning: EPOCH (epoch=N)
 * Source: epoch table
 * Output: epoch/epoch=N/data.parquet
 *
 * This table contains aggregate statistics for each epoch including
 * block count, transaction count, total output, fees, and timing information.
 *
 * The {@code epoch} rows are computed by the epoch-aggr module, asynchronously and only when
 * {@code store.epoch-aggr.enabled} and {@code store.epoch-aggr.epoch-calculation-enabled} are both
 * true. Without them the table has no rows, so this exporter is unavailable instead of storing every
 * epoch as an empty COMPLETED partition. With them, epoch N is ready once the chain is past it and
 * its row counts every block of epoch N.
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class EpochExporter extends AbstractTableExporter {

    private final BlockStorageReader blockStorageReader;
    private final EpochAggregateReader epochAggregateReader;
    /** Epochs whose row was already reported as stale or missing, so each is logged at WARN once. */
    private final Set<Integer> reported = ConcurrentHashMap.newKeySet();

    public EpochExporter(
            StorageWriter storageWriter,
            ExportStateService stateService,
            EraService eraService,
            AnalyticsStoreProperties properties,
            AdaPotJobStorage adaPotJobStorage,
            BlockStorageReader blockStorageReader,
            EpochAggregateReader epochAggregateReader) {
        super(storageWriter, stateService, eraService, properties, adaPotJobStorage);
        this.blockStorageReader = blockStorageReader;
        this.epochAggregateReader = epochAggregateReader;
    }

    @Override
    public Optional<String> unavailableReason() {
        if (!epochAggregateReader.isEpochCalculationEnabled()) {
            return Optional.of("its rows are computed by the epoch-aggr module, which needs "
                    + EpochAggregateReader.EPOCH_AGGR_ENABLED + "=true and "
                    + EpochAggregateReader.EPOCH_CALCULATION_ENABLED + "=true");
        }
        return Optional.empty();
    }

    /**
     * Epoch N is ready when the chain has moved past it and the {@code epoch} row of N counts as
     * many blocks as the block table holds for N. The epoch-aggr module recomputes the row after the
     * epoch has ended; until then the row covers only the blocks seen while N was current. An epoch
     * without blocks has no row, and its empty partition is final.
     */
    @Override
    public boolean requiresFinalizedEpoch() {
        return true;
    }

    @Override
    public boolean preExportValidation(PartitionValue partition) {
        int epoch = ((PartitionValue.EpochPartition) partition).epoch();
        int currentEpoch = blockStorageReader.findRecentBlock()
                .map(com.bloxbean.cardano.yaci.store.blocks.domain.Block::getEpochNumber)
                .orElse(-1);
        if (epoch >= currentEpoch) {
            return false;
        }

        try {
            int blocks = blockStorageReader.totalBlocksInEpoch(epoch);
            if (blocks == 0) {
                return true;
            }
            OptionalInt rowBlocks = epochAggregateReader.findBlockCount(epoch);
            if (rowBlocks.isPresent() && rowBlocks.getAsInt() == blocks) {
                return true;
            }
            // epoch-aggr recomputes epoch N only during epoch N+1. Once N+1 is over, a row that still
            // disagrees with the blocks will not be fixed (for example after a rollback across the
            // boundary): export it as it is rather than wait for ever.
            boolean settled = epoch < currentEpoch - 1;
            if (settled && rowBlocks.isPresent()) {
                if (reported.add(epoch)) {
                    log.warn("Epoch {} row counts {} blocks but {} are stored, and epoch-aggr no longer recomputes "
                            + "it; exporting the row as it is", epoch, rowBlocks.getAsInt(), blocks);
                }
                return true;
            }
            if (settled && reported.add(epoch)) {
                log.warn("Epoch {} has no row in the epoch table (epoch-aggr was off, or the database was restored "
                        + "without it), so it is not exported", epoch);
            } else {
                log.debug("Epoch {} row is not final yet ({} of {} blocks aggregated)", epoch,
                        rowBlocks.isPresent() ? rowBlocks.getAsInt() : "none", blocks);
            }
            return false;
        } catch (Exception e) {
            log.warn("Could not check the epoch row of epoch {}, not exporting it now (retried on the next run): {}",
                    epoch, e.getMessage());
            return false;
        }
    }

    /** Only an epoch without blocks has no row. */
    @Override
    protected boolean allowsEmptyPartition(PartitionValue partition) {
        return blockStorageReader.totalBlocksInEpoch(((PartitionValue.EpochPartition) partition).epoch()) == 0;
    }

    @Override
    public String getTableName() {
        return "epoch";
    }

    @Override
    public boolean includesByronEpochs() {
        return true;
    }

    @Override
    public PartitionStrategy getPartitionStrategy() {
        return PartitionStrategy.EPOCH;
    }

    /** Unified view: the exported {@code epoch} column is PostgreSQL {@code number}. */
    @Override
    public Map<String, String> getSourceColumnMappings() {
        return Map.of("epoch", "number");
    }

    @Override
    protected String buildQuery(PartitionValue partition, SlotRange slotRange) {
        String schema = getSourceSchema();
        int epoch = ((PartitionValue.EpochPartition) partition).epoch();
        
        return String.format("""
            SELECT * FROM postgres_query('source_db', '
                SELECT
                    e.number AS epoch,
                    e.block_count,
                    e.transaction_count,
                    e.total_output,
                    e.total_fees,
                    e.start_time,
                    e.end_time,
                    e.max_slot
                FROM %s.epoch e
                WHERE e.number = %d
            ')
            """,
            schema,
            epoch
        );
    }
}

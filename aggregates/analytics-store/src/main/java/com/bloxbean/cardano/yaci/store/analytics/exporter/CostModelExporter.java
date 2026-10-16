package com.bloxbean.cardano.yaci.store.analytics.exporter;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.adapot.job.storage.AdaPotJobStorage;
import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateService;
import com.bloxbean.cardano.yaci.store.analytics.writer.StorageWriter;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Exporter for Plutus cost models.
 *
 * Partitioning: DAILY (date=yyyy-MM-dd)
 * Source: cost_model table
 * Output: cost_model/date=yyyy-MM-dd/data.parquet
 *
 * This table stores the cost models used for Plutus script execution,
 * defining the resource costs for different Plutus operations.
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class CostModelExporter extends AbstractTableExporter {

    public CostModelExporter(
            StorageWriter storageWriter,
            ExportStateService stateService,
            EraService eraService,
            AnalyticsStoreProperties properties,
            AdaPotJobStorage adaPotJobStorage) {
        super(storageWriter, stateService, eraService, properties, adaPotJobStorage);
    }

    @Override
    public String getTableName() {
        return "cost_model";
    }

    @Override
    public PartitionStrategy getPartitionStrategy() {
        return PartitionStrategy.DAILY;
    }

    /**
     * Cost models are written with the epoch parameters: before Conway at the epoch transition, from
     * Conway on in the AdaPot job's pre-processing for the epoch, which can run long after the sync has
     * passed the day (about 10 hours behind during a mainnet initial sync). A day exported before then
     * misses the row for good, so a day waits for the AdaPot job of the last epoch it reaches. That
     * job always runs after the epoch transition, so the same rule is safe for every non-Byron epoch.
     */
    @Override
    public boolean preExportValidation(PartitionValue partition) {
        if (adaPotJobStorage instanceof NoOpAdaPotJobStorage) {
            return true;
        }
        long lastSlot = partition.toSlotRange(eraService).endSlot() - 1;
        if (lastSlot < eraService.getFirstNonByronSlot()) {
            return true;
        }
        return isRewardCalcAdaPotJobCompleted(eraService.getEpochNo(Era.Shelley, lastSlot));
    }

    @Override
    protected String buildQuery(PartitionValue partition, SlotRange slotRange) {
        String schema = getSourceSchema();
        return String.format("""
            SELECT * FROM postgres_query('source_db', '
                SELECT
                    cm.hash,
                    cm.costs,
                    cm.slot,
                    cm.block,
                    to_timestamp(COALESCE(cm.block_time, 0)) as block_time
                FROM %s.cost_model cm
                WHERE cm.slot >= %d
                  AND cm.slot < %d
                ORDER BY cm.slot
            ')
            """,
            schema,
            slotRange.startSlot(),
            slotRange.endSlot()
        );
    }
}

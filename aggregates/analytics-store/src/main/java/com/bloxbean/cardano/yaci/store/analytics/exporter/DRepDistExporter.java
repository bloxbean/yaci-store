package com.bloxbean.cardano.yaci.store.analytics.exporter;

import com.bloxbean.cardano.yaci.store.adapot.job.storage.AdaPotJobStorage;
import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateService;
import com.bloxbean.cardano.yaci.store.analytics.writer.StorageWriter;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Exporter for DRep voting power distribution per epoch.
 *
 * Partitioning: EPOCH (epoch=N)
 * Source: drep_dist table
 * Output: drep_dist/epoch=N/data.parquet
 *
 * This table contains the voting power (delegated stake) for each DRep per epoch.
 * Critical for governance analytics and voting power analysis.
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class DRepDistExporter extends AbstractTableExporter {

    public DRepDistExporter(
            StorageWriter storageWriter,
            ExportStateService stateService,
            EraService eraService,
            AnalyticsStoreProperties properties,
            AdaPotJobStorage adaPotJobStorage) {
        super(storageWriter, stateService, eraService, properties, adaPotJobStorage);
    }

    @Override
    public String getTableName() {
        return "drep_dist";
    }

    @Override
    public PartitionStrategy getPartitionStrategy() {
        return PartitionStrategy.EPOCH;
    }

    @Override
    public boolean preExportValidation(PartitionValue partition) {
        // DRep distribution depends on AdaPot job completion
        int epoch = ((PartitionValue.EpochPartition) partition).epoch();
        return isRewardCalcAdaPotJobCompleted(epoch);
    }

    @Override
    protected String buildQuery(PartitionValue partition, SlotRange slotRange) {
        String schema = getSourceSchema();
        int epoch = ((PartitionValue.EpochPartition) partition).epoch();
        
        return String.format("""
            SELECT
                dd.drep_hash,
                dd.drep_type,
                dd.drep_id,
                dd.amount,
                dd.epoch,
                dd.active_until,
                dd.expiry
            FROM source_db.%s.drep_dist dd
            WHERE dd.epoch = %d
            ORDER BY dd.epoch, dd.drep_hash
            """,
            schema,
            epoch
        );
    }
}

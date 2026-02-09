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
 * Exporter for staking rewards.
 *
 * Partitioning: EPOCH (epoch=N correspond to spendable_epoch)
 * Source: reward table
 * Output: reward/epoch=N/data.parquet
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class RewardExporter extends AbstractTableExporter {

    public RewardExporter(
            StorageWriter storageWriter,
            ExportStateService stateService,
            EraService eraService,
            AnalyticsStoreProperties properties,
            AdaPotJobStorage adaPotJobStorage) {
        super(storageWriter, stateService, eraService, properties, adaPotJobStorage);
    }

    @Override
    public String getTableName() {
        return "reward";
    }

    @Override
    public PartitionStrategy getPartitionStrategy() {
        return PartitionStrategy.EPOCH;
    }

    @Override
    public boolean preExportValidation(PartitionValue partition) {
        // Reward table depends on AdaPot job completion
        int epoch = ((PartitionValue.EpochPartition) partition).epoch();
        return isRewardCalcAdaPotJobCompleted(epoch);
    }

    @Override
    protected String buildQuery(PartitionValue partition, SlotRange slotRange) {
        String schema = getSourceSchema();
        int epoch = ((PartitionValue.EpochPartition) partition).epoch();

        // We use earned_epoch for partitioning rewards.
        return String.format("""
            SELECT
                r.address,
                r.earned_epoch AS epoch,
                r.spendable_epoch ,
                r.type,
                r.pool_id,
                r.amount,
                r.slot
            FROM source_db.%s.reward r
            WHERE r.earned_epoch = %d
            ORDER BY r.earned_epoch, r.address
            """,
            schema,
            epoch
        );
    }
}

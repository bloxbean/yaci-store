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
 * Exporter for epoch stake distribution.
 *
 * Partitioning: EPOCH (epoch=N)
 * Source: epoch_stake table
 * Output: epoch_stake/epoch=N/data.parquet
 *
 * This table contains the stake distribution snapshot for each epoch,
 * showing how much ADA each stake address has delegated to which pool.
 * Critical for staking analytics and rewards calculation verification.
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class EpochStakeExporter extends AbstractTableExporter {

    public EpochStakeExporter(
            StorageWriter storageWriter,
            ExportStateService stateService,
            EraService eraService,
            AnalyticsStoreProperties properties,
            AdaPotJobStorage adaPotJobStorage) {
        super(storageWriter, stateService, eraService, properties, adaPotJobStorage);
    }

    @Override
    public String getTableName() {
        return "epoch_stake";
    }

    @Override
    public PartitionStrategy getPartitionStrategy() {
        return PartitionStrategy.EPOCH;
    }

    @Override
    public boolean preExportValidation(PartitionValue partition) {
        // Epoch stake table depends on AdaPot job completion
        int epoch = ((PartitionValue.EpochPartition) partition).epoch();
        return isRewardCalcAdaPotJobCompleted(epoch);
    }

    @Override
    protected String buildQuery(PartitionValue partition, SlotRange slotRange) {
        String schema = getSourceSchema();
        int epoch = ((PartitionValue.EpochPartition) partition).epoch();
        
        return String.format("""
            SELECT
                es.epoch,
                es.address,
                es.amount,
                es.pool_id,
                es.delegation_epoch,
                es.active_epoch
            FROM source_db.%s.epoch_stake es
            WHERE es.epoch = %d
            ORDER BY es.epoch, es.address
            """,
            schema,
            epoch
        );
    }
}

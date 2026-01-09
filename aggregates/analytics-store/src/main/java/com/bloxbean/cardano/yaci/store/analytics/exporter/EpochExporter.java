package com.bloxbean.cardano.yaci.store.analytics.exporter;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateService;
import com.bloxbean.cardano.yaci.store.analytics.writer.StorageWriter;
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
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class EpochExporter extends AbstractTableExporter {

    public EpochExporter(
            StorageWriter storageWriter,
            ExportStateService stateService,
            EraService eraService,
            AnalyticsStoreProperties properties) {
        super(storageWriter, stateService, eraService, properties);
    }

    @Override
    public String getTableName() {
        return "epoch";
    }

    @Override
    public PartitionStrategy getPartitionStrategy() {
        return PartitionStrategy.EPOCH;
    }

    @Override
    protected String buildQuery(PartitionValue partition, SlotRange slotRange) {
        String schema = getSourceSchema();
        int epoch = ((PartitionValue.EpochPartition) partition).epoch();
        
        return String.format("""
            SELECT
                e.number AS epoch,
                e.block_count,
                e.transaction_count,
                e.total_output,
                e.total_fees,
                e.start_time,
                e.end_time,
                e.max_slot
            FROM source_db.%s.epoch e
            WHERE e.number = %d
            ORDER BY e.number
            """,
            schema,
            epoch
        );
    }
}

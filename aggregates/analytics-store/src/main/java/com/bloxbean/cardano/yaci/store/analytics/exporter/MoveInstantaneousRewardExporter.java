package com.bloxbean.cardano.yaci.store.analytics.exporter;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateService;
import com.bloxbean.cardano.yaci.store.analytics.writer.StorageWriter;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Exporter for Move Instantaneous Rewards (MIR).
 *
 * Partitioning: EPOCH (epoch=N)
 * Source: mir table
 * Output: mir/epoch=N/data.parquet
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class MoveInstantaneousRewardExporter extends AbstractTableExporter {

    public MoveInstantaneousRewardExporter(
            StorageWriter storageWriter,
            ExportStateService stateService,
            EraService eraService,
            AnalyticsStoreProperties properties) {
        super(storageWriter, stateService, eraService, properties);
    }

    @Override
    public String getTableName() {
        return "mir";
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
                m.tx_hash,
                m.cert_index,
                m.pot,
                m.credential,
                m.address,
                m.amount,
                m.epoch,
                m.slot,
                m.block_hash,
                to_timestamp(m.block_time) as block_time
            FROM source_db.%s.mir m
            WHERE m.epoch = %d
            ORDER BY m.slot, m.tx_hash, m.cert_index
            """,
            schema,
            epoch
        );
    }
}

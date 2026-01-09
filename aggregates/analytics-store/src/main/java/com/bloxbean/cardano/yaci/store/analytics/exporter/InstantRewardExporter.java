package com.bloxbean.cardano.yaci.store.analytics.exporter;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateService;
import com.bloxbean.cardano.yaci.store.analytics.writer.StorageWriter;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Exporter for instant rewards (MIR certificates).
 *
 * Partitioning: EPOCH (epoch=N by spendable_epoch)
 * Source: instant_reward table
 * Output: instant_reward/epoch=N/data.parquet
 *
 * This table tracks instant rewards from Move Instantaneous Rewards (MIR)
 * certificates, typically used for treasury withdrawals or reserve distributions.
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class InstantRewardExporter extends AbstractTableExporter {

    public InstantRewardExporter(
            StorageWriter storageWriter,
            ExportStateService stateService,
            EraService eraService,
            AnalyticsStoreProperties properties) {
        super(storageWriter, stateService, eraService, properties);
    }

    @Override
    public String getTableName() {
        return "instant_reward";
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
                ir.address,
                ir.type,
                ir.amount,
                ir.earned_epoch,
                ir.spendable_epoch AS epoch,
                ir.slot
            FROM source_db.%s.instant_reward ir
            WHERE ir.spendable_epoch = %d
            ORDER BY ir.spendable_epoch, ir.address
            """,
            schema,
            epoch
        );
    }
}

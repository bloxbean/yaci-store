package com.bloxbean.cardano.yaci.store.analytics.exporter;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateService;
import com.bloxbean.cardano.yaci.store.analytics.writer.StorageWriter;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Exporter for reward rest (remaining rewards on deregistered stake addresses).
 *
 * This exporter tracks rewards that remain in the reward pot for stake addresses
 * that have been deregistered. These rewards can be reclaimed when the stake
 * address is registered again.
 *
 * Partitioning: EPOCH (epoch=N based on earned_epoch)
 * Source: reward_rest table
 * Output: reward_rest/epoch=N/data.parquet
 *
 * Use cases:
 * - Complete reward analysis including deregistered stakes
 * - Tracking potential reclaim amounts
 * - Stake lifecycle analysis
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class RewardRestExporter extends AbstractTableExporter {

    public RewardRestExporter(
            StorageWriter storageWriter,
            ExportStateService stateService,
            EraService eraService,
            AnalyticsStoreProperties properties) {
        super(storageWriter, stateService, eraService, properties);
    }

    @Override
    public String getTableName() {
        return "reward_rest";
    }

    @Override
    public PartitionStrategy getPartitionStrategy() {
        return PartitionStrategy.EPOCH;
    }

    /**
     * Build SQL query for reward rest data by earned epoch.
     */
    @Override
    protected String buildQuery(PartitionValue partition, SlotRange slotRange) {
        String schema = getSourceSchema();
        int epoch = ((PartitionValue.EpochPartition) partition).epoch();

        return String.format("""
            SELECT
                rr.id,
                rr.address,
                rr.type,
                rr.earned_epoch as epoch,
                rr.amount,
                rr.spendable_epoch,
                rr.slot
            FROM source_db.%s.reward_rest rr
            WHERE rr.earned_epoch = %d
            ORDER BY rr.earned_epoch, rr.address
            """,
            schema,
            epoch
        );
    }
}

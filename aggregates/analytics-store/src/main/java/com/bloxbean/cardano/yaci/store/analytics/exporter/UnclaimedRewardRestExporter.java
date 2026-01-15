package com.bloxbean.cardano.yaci.store.analytics.exporter;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateService;
import com.bloxbean.cardano.yaci.store.analytics.writer.StorageWriter;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Exporter for unclaimed reward rest (rewards not claimed before treasury transfer).
 *
 * This exporter tracks rewards that remain unclaimed and may be transferred to treasury.
 * These are rewards from deregistered stake addresses that were never reclaimed.
 *
 * Partitioning: EPOCH (epoch=N based on earned_epoch)
 * Source: unclaimed_reward_rest table
 * Output: unclaimed_reward_rest/epoch=N/data.parquet
 *
 * Use cases:
 * - Tracking unclaimed staking rewards
 * - Treasury analysis (funds transferred from unclaimed)
 * - Reward lifecycle completion
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class UnclaimedRewardRestExporter extends AbstractTableExporter {

    public UnclaimedRewardRestExporter(
            StorageWriter storageWriter,
            ExportStateService stateService,
            EraService eraService,
            AnalyticsStoreProperties properties) {
        super(storageWriter, stateService, eraService, properties);
    }

    @Override
    public String getTableName() {
        return "unclaimed_reward_rest";
    }

    @Override
    public PartitionStrategy getPartitionStrategy() {
        return PartitionStrategy.EPOCH;
    }

    /**
     * Build SQL query for unclaimed reward rest data by earned epoch.
     */
    @Override
    protected String buildQuery(PartitionValue partition, SlotRange slotRange) {
        String schema = getSourceSchema();
        int epoch = ((PartitionValue.EpochPartition) partition).epoch();

        return String.format("""
            SELECT
                urr.id,
                urr.address,
                urr.type,
                urr.earned_epoch,
                urr.amount,
                urr.spendable_epoch,
                urr.slot
            FROM source_db.%s.unclaimed_reward_rest urr
            WHERE urr.earned_epoch = %d
            ORDER BY urr.earned_epoch, urr.address
            """,
            schema,
            epoch
        );
    }
}

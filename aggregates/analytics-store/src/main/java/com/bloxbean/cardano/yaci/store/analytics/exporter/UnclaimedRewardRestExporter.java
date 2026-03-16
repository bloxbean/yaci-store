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
            AnalyticsStoreProperties properties,
            AdaPotJobStorage adaPotJobStorage) {
        super(storageWriter, stateService, eraService, properties, adaPotJobStorage);
    }

    @Override
    public String getTableName() {
        return "unclaimed_reward_rest";
    }

    @Override
    public PartitionStrategy getPartitionStrategy() {
        return PartitionStrategy.EPOCH;
    }

    @Override
    public boolean preExportValidation(PartitionValue partition) {
        // unclaimed_reward_rest[earned_epoch=N] is populated at the start of AdaPot job N+1 via
        // PreAdaPotJobProcessingEvent(N+1), which triggers TreasuryWithdrawalProcessor and
        // ProposalRefundProcessor with epoch = N. Wait for job N+1 to complete before exporting epoch N.
        int epoch = ((PartitionValue.EpochPartition) partition).epoch();
        return isRewardCalcAdaPotJobCompleted(epoch + 1);
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
                urr.earned_epoch as epoch,
                urr.amount,
                urr.spendable_epoch,
                urr.slot
            FROM source_db.%s.unclaimed_reward_rest urr
            WHERE urr.earned_epoch = %d
            """,
            schema,
            epoch
        );
    }
}

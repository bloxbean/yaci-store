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
 * Exporter for Ada pots (treasury, reserves, circulation, etc.).
 *
 * Partitioning: EPOCH (epoch=N)
 * Source: adapot table
 * Output: adapot/epoch=N/data.parquet
 *
 * This table tracks the distribution of ADA across different "pots" in the Cardano system:
 * - Treasury: Funds available for governance proposals
 * - Reserves: Unminted ADA supply
 * - Circulation: ADA in active circulation
 * - Deposits: Stake key and pool deposits
 * - Fees: Transaction fees collected
 * - Rewards: Distributed and undistributed staking rewards
 *
 * Critical for economic analysis and treasury tracking.
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class AdaPotExporter extends AbstractTableExporter {

    public AdaPotExporter(
            StorageWriter storageWriter,
            ExportStateService stateService,
            EraService eraService,
            AnalyticsStoreProperties properties,
            AdaPotJobStorage adaPotJobStorage) {
        super(storageWriter, stateService, eraService, properties, adaPotJobStorage);
    }

    @Override
    public String getTableName() {
        return "adapot";
    }

    @Override
    public PartitionStrategy getPartitionStrategy() {
        return PartitionStrategy.EPOCH;
    }

    @Override
    public boolean preExportValidation(PartitionValue partition) {
        // AdaPot table depends on AdaPot job completion
        int epoch = ((PartitionValue.EpochPartition) partition).epoch();
        return isRewardCalcAdaPotJobCompleted(epoch);
    }

    @Override
    protected String buildQuery(PartitionValue partition, SlotRange slotRange) {
        String schema = getSourceSchema();
        int epoch = ((PartitionValue.EpochPartition) partition).epoch();
        
        return String.format("""
            SELECT
                ap.epoch,
                ap.slot,
                ap.deposits_stake,
                ap.fees,
                ap.utxo,
                ap.treasury,
                ap.reserves,
                ap.circulation,
                ap.distributed_rewards,
                ap.undistributed_rewards,
                ap.rewards_pot,
                ap.pool_rewards_pot
            FROM source_db.%s.adapot ap
            WHERE ap.epoch = %d
            ORDER BY ap.epoch
            """,
            schema,
            epoch
        );
    }
}

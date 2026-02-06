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
 * Exporter for constitutional committee information.
 *
 * Partitioning: EPOCH (epoch=N)
 * Source: committee table
 * Output: committee/epoch=N/data.parquet
 *
 * This table tracks the constitutional committee configuration including
 * threshold settings for governance actions.
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class CommitteeExporter extends AbstractTableExporter {

    public CommitteeExporter(
            StorageWriter storageWriter,
            ExportStateService stateService,
            EraService eraService,
            AnalyticsStoreProperties properties,
            AdaPotJobStorage adaPotJobStorage) {
        super(storageWriter, stateService, eraService, properties, adaPotJobStorage);
    }

    @Override
    public String getTableName() {
        return "committee";
    }

    @Override
    public PartitionStrategy getPartitionStrategy() {
        return PartitionStrategy.EPOCH;
    }

    @Override
    public boolean preExportValidation(PartitionValue partition) {
        // Committee is updated during AdaPot job processing (PreAdaPotJobProcessingEvent)
        int epoch = ((PartitionValue.EpochPartition) partition).epoch();
        return isRewardCalcAdaPotJobCompleted(epoch);
    }

    @Override
    protected String buildQuery(PartitionValue partition, SlotRange slotRange) {
        String schema = getSourceSchema();
        int epoch = ((PartitionValue.EpochPartition) partition).epoch();
        
        return String.format("""
            SELECT
                c.gov_action_tx_hash,
                c.gov_action_index,
                c.threshold_numerator,
                c.threshold_denominator,
                c.threshold,
                c.epoch,
                c.slot
            FROM source_db.%s.committee c
            WHERE c.epoch = %d
            ORDER BY c.epoch
            """,
            schema,
            epoch
        );
    }
}

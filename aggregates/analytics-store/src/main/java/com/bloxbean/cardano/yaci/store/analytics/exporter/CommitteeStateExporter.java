package com.bloxbean.cardano.yaci.store.analytics.exporter;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateService;
import com.bloxbean.cardano.yaci.store.analytics.writer.StorageWriter;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Exporter for constitutional committee state per epoch.
 *
 * Partitioning: EPOCH (epoch=N)
 * Source: committee_state table
 * Output: committee_state/epoch=N/data.parquet
 *
 * This table tracks the overall state of the constitutional committee
 * (e.g., "NoConfidence", "Normal") for governance purposes.
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class CommitteeStateExporter extends AbstractTableExporter {

    public CommitteeStateExporter(
            StorageWriter storageWriter,
            ExportStateService stateService,
            EraService eraService,
            AnalyticsStoreProperties properties) {
        super(storageWriter, stateService, eraService, properties);
    }

    @Override
    public String getTableName() {
        return "committee_state";
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
                cs.epoch,
                cs.state
            FROM source_db.%s.committee_state cs
            WHERE cs.epoch = %d
            ORDER BY cs.epoch
            """,
            schema,
            epoch
        );
    }
}

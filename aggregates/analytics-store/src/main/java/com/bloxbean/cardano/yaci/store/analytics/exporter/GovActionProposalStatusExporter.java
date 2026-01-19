package com.bloxbean.cardano.yaci.store.analytics.exporter;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateService;
import com.bloxbean.cardano.yaci.store.analytics.writer.StorageWriter;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Exporter for governance action proposal status per epoch.
 *
 * Partitioning: EPOCH (epoch=N)
 * Source: gov_action_proposal_status table
 * Output: gov_action_proposal_status/epoch=N/data.parquet
 *
 * This table tracks the status of governance proposals including
 * voting statistics and current state (active, ratified, expired, etc.).
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class GovActionProposalStatusExporter extends AbstractTableExporter {

    public GovActionProposalStatusExporter(
            StorageWriter storageWriter,
            ExportStateService stateService,
            EraService eraService,
            AnalyticsStoreProperties properties) {
        super(storageWriter, stateService, eraService, properties);
    }

    @Override
    public String getTableName() {
        return "gov_action_proposal_status";
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
                gaps.gov_action_tx_hash,
                gaps.gov_action_index,
                gaps.type,
                gaps.status,
                gaps.voting_stats,
                gaps.epoch
            FROM source_db.%s.gov_action_proposal_status gaps
            WHERE gaps.epoch = %d
            ORDER BY gaps.epoch, gaps.gov_action_tx_hash, gaps.gov_action_index
            """,
            schema,
            epoch
        );
    }
}

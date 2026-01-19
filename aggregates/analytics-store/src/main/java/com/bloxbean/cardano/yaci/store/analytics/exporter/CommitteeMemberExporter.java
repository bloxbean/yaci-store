package com.bloxbean.cardano.yaci.store.analytics.exporter;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateService;
import com.bloxbean.cardano.yaci.store.analytics.writer.StorageWriter;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Exporter for committee members.
 *
 * Partitioning: EPOCH (epoch=N)
 * Source: committee_member table
 * Output: committee_member/epoch=N/data.parquet
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class CommitteeMemberExporter extends AbstractTableExporter {

    public CommitteeMemberExporter(
            StorageWriter storageWriter,
            ExportStateService stateService,
            EraService eraService,
            AnalyticsStoreProperties properties) {
        super(storageWriter, stateService, eraService, properties);
    }

    @Override
    public String getTableName() {
        return "committee_member";
    }

    @Override
    public PartitionStrategy getPartitionStrategy() {
        return PartitionStrategy.EPOCH;
    }

    @Override
    protected String buildQuery(PartitionValue partition, SlotRange slotRange) {
        String schema = getSourceSchema();
        return String.format("""
            SELECT
                cm.hash,
                cm.cred_type,
                cm.start_epoch,
                cm.expired_epoch,
                cm.epoch,
                cm.slot,
                cm.update_datetime
            FROM source_db.%s.committee_member cm
            WHERE cm.slot >= %d
              AND cm.slot < %d
            ORDER BY cm.slot, cm.hash
            """,
            schema,
            slotRange.startSlot(),
            slotRange.endSlot()
        );
    }
}

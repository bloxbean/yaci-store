package com.bloxbean.cardano.yaci.store.analytics.exporter;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateService;
import com.bloxbean.cardano.yaci.store.analytics.writer.StorageWriter;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Exporter for committee registrations.
 *
 * Partitioning: DAILY (date=yyyy-MM-dd)
 * Source: committee_registration table
 * Output: committee_registration/date=yyyy-MM-dd/data.parquet
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class CommitteeRegistrationExporter extends AbstractTableExporter {

    public CommitteeRegistrationExporter(
            StorageWriter storageWriter,
            ExportStateService stateService,
            EraService eraService,
            AnalyticsStoreProperties properties) {
        super(storageWriter, stateService, eraService, properties);
    }

    @Override
    public String getTableName() {
        return "committee_registration";
    }

    @Override
    public PartitionStrategy getPartitionStrategy() {
        return PartitionStrategy.DAILY;
    }

    @Override
    protected String buildQuery(PartitionValue partition, SlotRange slotRange) {
        String schema = getSourceSchema();
        return String.format("""
            SELECT
                cr.tx_hash,
                cr.cert_index,
                cr.tx_index,
                cr.slot,
                cr.cold_key,
                cr.hot_key,
                cr.cred_type,
                cr.epoch,
                cr.block,
                to_timestamp(cr.block_time) as block_time
            FROM source_db.%s.committee_registration cr
            WHERE cr.slot >= %d
              AND cr.slot < %d
            ORDER BY cr.slot, cr.tx_hash, cr.cert_index
            """,
            schema,
            slotRange.startSlot(),
            slotRange.endSlot()
        );
    }
}

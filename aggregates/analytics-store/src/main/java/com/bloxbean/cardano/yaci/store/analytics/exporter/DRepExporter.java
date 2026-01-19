package com.bloxbean.cardano.yaci.store.analytics.exporter;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateService;
import com.bloxbean.cardano.yaci.store.analytics.writer.StorageWriter;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Exporter for DRep (Delegated Representative) state aggregate.
 *
 * Partitioning: DAILY (date=yyyy-MM-dd)
 * Source: drep table
 * Output: drep/date=yyyy-MM-dd/data.parquet
 *
 * This table tracks DRep registration/deregistration events and current status.
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class DRepExporter extends AbstractTableExporter {

    public DRepExporter(
            StorageWriter storageWriter,
            ExportStateService stateService,
            EraService eraService,
            AnalyticsStoreProperties properties) {
        super(storageWriter, stateService, eraService, properties);
    }

    @Override
    public String getTableName() {
        return "drep";
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
                d.drep_id,
                d.drep_hash,
                d.tx_hash,
                d.cert_index,
                d.tx_index,
                d.cert_type,
                d.status,
                d.deposit,
                d.epoch,
                d.registration_slot,
                d.slot,
                d.block_hash,
                d.block,
                to_timestamp(d.block_time) as block_time
            FROM source_db.%s.drep d
            WHERE d.slot >= %d
              AND d.slot < %d
            ORDER BY d.slot, d.drep_hash
            """,
            schema,
            slotRange.startSlot(),
            slotRange.endSlot()
        );
    }
}

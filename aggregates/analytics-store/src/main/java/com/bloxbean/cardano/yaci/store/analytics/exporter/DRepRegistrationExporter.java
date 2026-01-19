package com.bloxbean.cardano.yaci.store.analytics.exporter;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateService;
import com.bloxbean.cardano.yaci.store.analytics.writer.StorageWriter;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Exporter for DRep registrations.
 *
 * Partitioning: DAILY (date=yyyy-MM-dd)
 * Source: drep_registration table
 * Output: drep_registration/date=yyyy-MM-dd/data.parquet
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class DRepRegistrationExporter extends AbstractTableExporter {

    public DRepRegistrationExporter(
            StorageWriter storageWriter,
            ExportStateService stateService,
            EraService eraService,
            AnalyticsStoreProperties properties) {
        super(storageWriter, stateService, eraService, properties);
    }

    @Override
    public String getTableName() {
        return "drep_registration";
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
                dr.tx_hash,
                dr.cert_index,
                dr.tx_index,
                dr.slot,
                dr.drep_hash,
                dr.drep_id,
                dr.deposit,
                dr.type,
                dr.anchor_url,
                dr.anchor_hash,
                dr.cred_type,
                dr.epoch,
                dr.block,
                to_timestamp(dr.block_time) as block_time
            FROM source_db.%s.drep_registration dr
            WHERE dr.slot >= %d
              AND dr.slot < %d
            ORDER BY dr.slot, dr.tx_hash, dr.cert_index
            """,
            schema,
            slotRange.startSlot(),
            slotRange.endSlot()
        );
    }
}

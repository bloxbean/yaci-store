package com.bloxbean.cardano.yaci.store.analytics.exporter;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateService;
import com.bloxbean.cardano.yaci.store.analytics.writer.StorageWriter;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Exporter for committee deregistrations.
 *
 * Partitioning: DAILY (date=yyyy-MM-dd)
 * Source: committee_deregistration table
 * Output: committee_deregistration/date=yyyy-MM-dd/data.parquet
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class CommitteeDeRegistrationExporter extends AbstractTableExporter {

    public CommitteeDeRegistrationExporter(
            StorageWriter storageWriter,
            ExportStateService stateService,
            EraService eraService,
            AnalyticsStoreProperties properties) {
        super(storageWriter, stateService, eraService, properties);
    }

    @Override
    public String getTableName() {
        return "committee_deregistration";
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
                cd.tx_hash,
                cd.cert_index,
                cd.tx_index,
                cd.slot,
                cd.anchor_url,
                cd.anchor_hash,
                cd.cold_key,
                cd.cred_type,
                cd.epoch,
                cd.block,
                to_timestamp(cd.block_time) as block_time
            FROM source_db.%s.committee_deregistration cd
            WHERE cd.slot >= %d
              AND cd.slot < %d
            ORDER BY cd.slot, cd.tx_hash, cd.cert_index
            """,
            schema,
            slotRange.startSlot(),
            slotRange.endSlot()
        );
    }
}

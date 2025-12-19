package com.bloxbean.cardano.yaci.store.analytics.exporter;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateService;
import com.bloxbean.cardano.yaci.store.analytics.writer.StorageWriter;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Exporter for stake delegations.
 *
 * Partitioning: DAILY (date=yyyy-MM-dd)
 * Source: delegation table
 * Output: delegation/date=2024-01-15/data.parquet
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class DelegationExporter extends AbstractTableExporter {

    public DelegationExporter(
            StorageWriter storageWriter,
            ExportStateService stateService,
            EraService eraService,
            AnalyticsStoreProperties properties) {
        super(storageWriter, stateService, eraService, properties);
    }

    @Override
    public String getTableName() {
        return "delegation";
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
                d.tx_hash,
                d.cert_index,
                d.tx_index,
                d.credential,
                d.cred_type,
                d.pool_id,
                d.address,
                d.epoch,
                d.slot,
                d.block_hash,
                d.block,
                to_timestamp(d.block_time) as block_time
            FROM source_db.%s.delegation d
            WHERE d.slot >= %d
              AND d.slot < %d
            ORDER BY d.slot, d.tx_index, d.cert_index
            """,
            schema,
            slotRange.startSlot(),
            slotRange.endSlot()
        );
    }
}


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
 * Exporter for pool state aggregate.
 *
 * Partitioning: DAILY (date=yyyy-MM-dd)
 * Source: pool table
 * Output: pool/date=yyyy-MM-dd/data.parquet
 *
 * This table tracks the current state of stake pools including
 * registration status, active/retire epochs, and pledge amounts.
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class PoolExporter extends AbstractTableExporter {

    public PoolExporter(
            StorageWriter storageWriter,
            ExportStateService stateService,
            EraService eraService,
            AnalyticsStoreProperties properties,
            AdaPotJobStorage adaPotJobStorage) {
        super(storageWriter, stateService, eraService, properties, adaPotJobStorage);
    }

    @Override
    public String getTableName() {
        return "pool";
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
                p.pool_id,
                p.tx_hash,
                p.cert_index,
                p.tx_index,
                p.status,
                p.amount,
                p.epoch,
                p.active_epoch,
                p.retire_epoch,
                p.registration_slot,
                p.slot,
                p.block_hash,
                p.block,
                to_timestamp(p.block_time) as block_time
            FROM source_db.%s.pool p
            WHERE p.slot >= %d
              AND p.slot < %d
            ORDER BY p.slot, p.pool_id
            """,
            schema,
            slotRange.startSlot(),
            slotRange.endSlot()
        );
    }
}

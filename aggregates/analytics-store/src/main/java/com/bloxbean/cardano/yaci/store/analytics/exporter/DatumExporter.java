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
 * Exporter for datum values.
 *
 * Partitioning: DAILY (date=yyyy-MM-dd)
 * Source: datum table
 * Output: datum/date=yyyy-MM-dd/data.parquet
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class DatumExporter extends AbstractTableExporter {

    public DatumExporter(
            StorageWriter storageWriter,
            ExportStateService stateService,
            EraService eraService,
            AnalyticsStoreProperties properties,
            AdaPotJobStorage adaPotJobStorage) {
        super(storageWriter, stateService, eraService, properties, adaPotJobStorage);
    }

    @Override
    public String getTableName() {
        return "datum";
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
                d.hash,
                d.datum,
                d.created_at_tx,
                d.slot,
                to_timestamp(COALESCE(b.block_time, 0)) as block_time
            FROM source_db.%s.datum d
            INNER JOIN source_db.%s.block b ON d.slot = b.slot
            WHERE d.slot >= %d
              AND d.slot < %d
            ORDER BY d.slot, d.hash
            """,
            schema, schema,
            slotRange.startSlot(),
            slotRange.endSlot()
        );
    }
}

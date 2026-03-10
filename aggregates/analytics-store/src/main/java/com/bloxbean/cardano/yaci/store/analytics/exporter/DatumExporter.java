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
    public String getPartitionColumn() {
        return "date";
    }

    @Override
    protected String buildQuery(PartitionValue partition, SlotRange slotRange) {
        String schema = getSourceSchema();
        String dateStr = ((PartitionValue.DatePartition) partition).date().toString();
        return String.format("""
            SELECT
                d.hash,
                d.datum,
                d.created_at_tx,
                d.slot,
                CAST('%s' AS DATE) as date
            FROM source_db.%s.datum d
            WHERE d.slot >= %d
              AND d.slot < %d
            ORDER BY d.slot, d.hash
            """,
            dateStr, schema,
            slotRange.startSlot(),
            slotRange.endSlot()
        );
    }
}

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
 * Exporter for scripts.
 *
 * Partitioning: DAILY (date=yyyy-MM-dd)
 * Source: script table
 * Output: script/date=yyyy-MM-dd/data.parquet
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class ScriptExporter extends AbstractTableExporter {

    public ScriptExporter(
            StorageWriter storageWriter,
            ExportStateService stateService,
            EraService eraService,
            AnalyticsStoreProperties properties,
            AdaPotJobStorage adaPotJobStorage) {
        super(storageWriter, stateService, eraService, properties, adaPotJobStorage);
    }

    @Override
    public String getTableName() {
        return "script";
    }

    @Override
    public PartitionStrategy getPartitionStrategy() {
        return PartitionStrategy.DAILY;
    }

    @Override
    public String getPartitionColumn() {
        return "block_date";
    }

    @Override
    protected String buildQuery(PartitionValue partition, SlotRange slotRange) {
        String schema = getSourceSchema();
        String dateStr = ((PartitionValue.DatePartition) partition).date().toString();
        return String.format("""
            SELECT
                s.script_hash,
                s.script_type,
                s.content,
                s.slot,
                CAST('%s' AS DATE) as block_date
            FROM source_db.%s.script s
            WHERE s.slot >= %d
              AND s.slot < %d
            ORDER BY s.slot, s.script_hash
            """,
            dateStr, schema,
            slotRange.startSlot(),
            slotRange.endSlot()
        );
    }
}

package com.bloxbean.cardano.yaci.store.analytics.exporter;

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
            AnalyticsStoreProperties properties) {
        super(storageWriter, stateService, eraService, properties);
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
    protected String buildQuery(PartitionValue partition, SlotRange slotRange) {
        String schema = getSourceSchema();
        return String.format("""
            SELECT DISTINCT
                s.script_hash,
                s.script_type,
                s.content,
                -- We align with transaction_scripts slot to drive the partition
                ts.slot,
                to_timestamp(ts.block_time) as block_time
            FROM source_db.%s.transaction_scripts ts
            INNER JOIN source_db.%s.script s ON s.script_hash = ts.script_hash
            WHERE ts.slot >= %d
              AND ts.slot < %d
            ORDER BY ts.slot, s.script_hash
            """,
            schema, schema,
            slotRange.startSlot(),
            slotRange.endSlot()
        );
    }
}

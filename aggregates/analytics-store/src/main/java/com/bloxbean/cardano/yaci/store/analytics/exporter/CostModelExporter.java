package com.bloxbean.cardano.yaci.store.analytics.exporter;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateService;
import com.bloxbean.cardano.yaci.store.analytics.writer.StorageWriter;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Exporter for Plutus cost models.
 *
 * Partitioning: DAILY (date=yyyy-MM-dd)
 * Source: cost_model table
 * Output: cost_model/date=yyyy-MM-dd/data.parquet
 *
 * This table stores the cost models used for Plutus script execution,
 * defining the resource costs for different Plutus operations.
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class CostModelExporter extends AbstractTableExporter {

    public CostModelExporter(
            StorageWriter storageWriter,
            ExportStateService stateService,
            EraService eraService,
            AnalyticsStoreProperties properties) {
        super(storageWriter, stateService, eraService, properties);
    }

    @Override
    public String getTableName() {
        return "cost_model";
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
                cm.hash,
                cm.costs,
                cm.slot,
                cm.block,
                to_timestamp(cm.block_time) as block_time
            FROM source_db.%s.cost_model cm
            WHERE cm.slot >= %d
              AND cm.slot < %d
            ORDER BY cm.slot, cm.hash
            """,
            schema,
            slotRange.startSlot(),
            slotRange.endSlot()
        );
    }
}

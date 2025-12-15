package com.bloxbean.cardano.yaci.store.analytics.exporter;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateService;
import com.bloxbean.cardano.yaci.store.analytics.writer.StorageWriter;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Exporter for transaction scripts and redeemer details.
 *
 * Partitioning: DAILY (date=yyyy-MM-dd)
 * Source: transaction_scripts table
 * Output: transaction_scripts/date=2024-01-15/data.parquet
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class TransactionScriptsExporter extends AbstractTableExporter {

    public TransactionScriptsExporter(
            StorageWriter storageWriter,
            ExportStateService stateService,
            EraService eraService,
            AnalyticsStoreProperties properties) {
        super(storageWriter, stateService, eraService, properties);
    }

    @Override
    public String getTableName() {
        return "transaction_scripts";
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
                ts.id,
                ts.slot,
                ts.block_hash,
                ts.tx_hash,
                ts.script_hash,
                ts.script_type,
                ts.datum_hash,
                ts.redeemer_cbor,
                ts.unit_mem,
                ts.unit_steps,
                ts.purpose,
                ts.redeemer_index,
                ts.redeemer_datahash,
                ts.block,
                to_timestamp(ts.block_time) as block_time
            FROM source_db.%s.transaction_scripts ts
            WHERE ts.slot >= %d
              AND ts.slot < %d
            ORDER BY ts.slot, ts.tx_hash, ts.redeemer_index
            """,
            schema,
            slotRange.startSlot(),
            slotRange.endSlot()
        );
    }
}


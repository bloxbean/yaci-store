package com.bloxbean.cardano.yaci.store.analytics.exporter;

import com.bloxbean.cardano.yaci.store.adapot.job.storage.AdaPotJobStorage;
import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateService;
import com.bloxbean.cardano.yaci.store.analytics.writer.StorageWriter;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

/**
 * Exporter for block CBOR, written only when the store saves it (ADR 0006).
 *
 * Partitioning: DAILY (date=yyyy-MM-dd)
 * Source: block_cbor table (joined with block for block_time)
 * Output: block_cbor/date=2024-01-15/data.parquet
 */
@Service
@Slf4j
@ConditionalOnExpression("${yaci.store.analytics.enabled:false} and (${store.blocks.save-cbor:false})")
public class BlockCborExporter extends AbstractTableExporter {

    public BlockCborExporter(
            StorageWriter storageWriter,
            ExportStateService stateService,
            EraService eraService,
            AnalyticsStoreProperties properties,
            AdaPotJobStorage adaPotJobStorage) {
        super(storageWriter, stateService, eraService, properties, adaPotJobStorage);
    }

    @Override
    public String getTableName() {
        return "block_cbor";
    }

    @Override
    public PartitionStrategy getPartitionStrategy() {
        return PartitionStrategy.DAILY;
    }

    @Override
    protected String buildQuery(PartitionValue partition, SlotRange slotRange) {
        String schema = getSourceSchema();
        return String.format("""
            SELECT * FROM postgres_query('source_db', '
                SELECT
                    c.block_hash,
                    c.cbor_data,
                    c.cbor_size,
                    c.slot,
                    b.number, b.epoch, to_timestamp(COALESCE(b.block_time, 0)) as block_time
                FROM %s.block_cbor c
                INNER JOIN %s.block b
                    ON b.hash = c.block_hash
                WHERE c.slot >= %d
                  AND c.slot < %d
                ORDER BY c.slot
            ')
            """,
            schema, schema,
            slotRange.startSlot(),
            slotRange.endSlot()
        );
    }
}

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
 * Exporter for transaction CBOR, written only when the store saves it (ADR 0006).
 *
 * Partitioning: DAILY (date=yyyy-MM-dd)
 * Source: transaction_cbor table (joined with transaction for block_time)
 * Output: transaction_cbor/date=2024-01-15/data.parquet
 */
@Service
@Slf4j
@ConditionalOnExpression("${yaci.store.analytics.enabled:false} and (${store.transaction.save-cbor:false} or ${store.transaction.save-full-tx-cbor:false})")
public class TransactionCborExporter extends AbstractTableExporter {

    public TransactionCborExporter(
            StorageWriter storageWriter,
            ExportStateService stateService,
            EraService eraService,
            AnalyticsStoreProperties properties,
            AdaPotJobStorage adaPotJobStorage) {
        super(storageWriter, stateService, eraService, properties, adaPotJobStorage);
    }

    @Override
    public String getTableName() {
        return "transaction_cbor";
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
                    c.tx_hash,
                    c.cbor_data,
                    c.cbor_size,
                    c.slot,
                    t.block, t.epoch, to_timestamp(COALESCE(t.block_time, 0)) as block_time
                FROM %s.transaction_cbor c
                INNER JOIN %s.transaction t
                    ON t.tx_hash = c.tx_hash
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

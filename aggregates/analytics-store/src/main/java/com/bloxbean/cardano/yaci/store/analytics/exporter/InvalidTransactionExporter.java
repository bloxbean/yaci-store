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
 * Exporter for invalid transactions.
 *
 * This exporter tracks transactions that failed validation during processing.
 * Useful for:
 * - Audit and compliance purposes
 * - Debugging transaction issues
 * - Analyzing invalid transaction patterns
 *
 * Partitioning: DAILY (date=yyyy-MM-dd)
 * Source: invalid_transaction table
 * Output: invalid_transaction/date=yyyy-MM-dd/data.parquet
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class InvalidTransactionExporter extends AbstractTableExporter {

    public InvalidTransactionExporter(
            StorageWriter storageWriter,
            ExportStateService stateService,
            EraService eraService,
            AnalyticsStoreProperties properties,
            AdaPotJobStorage adaPotJobStorage) {
        super(storageWriter, stateService, eraService, properties, adaPotJobStorage);
    }

    @Override
    public String getTableName() {
        return "invalid_transaction";
    }

    @Override
    public PartitionStrategy getPartitionStrategy() {
        return PartitionStrategy.DAILY;
    }

    /**
     * Build SQL query for invalid transactions in the slot range.
     *
     * Exports transaction hash, slot, block hash, and transaction JSON.
     * JOINs with block table to get block_time for date partitioning.
     */
    @Override
    protected String buildQuery(PartitionValue partition, SlotRange slotRange) {
        String schema = getSourceSchema();
        return String.format("""
            SELECT
                it.tx_hash,
                it.slot,
                it.block_hash,
                it.transaction::text as transaction,
                to_timestamp(b.block_time) as block_time
            FROM source_db.%s.invalid_transaction it
            INNER JOIN source_db.%s.block b ON it.slot = b.slot
            WHERE it.slot >= %d
              AND it.slot < %d
            ORDER BY it.slot, it.tx_hash
            """,
            schema, schema,
            slotRange.startSlot(),
            slotRange.endSlot()
        );
    }
}

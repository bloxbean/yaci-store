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
 * Exporter for transaction metadata (CIP-20).
 *
 * Partitioning: DAILY (date=yyyy-MM-dd)
 * Source: transaction_metadata table (stores/metadata)
 * Output: transaction_metadata/date=2024-01-15/data.parquet
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class TransactionMetadataExporter extends AbstractTableExporter {

    public TransactionMetadataExporter(
            StorageWriter storageWriter,
            ExportStateService stateService,
            EraService eraService,
            AnalyticsStoreProperties properties,
            AdaPotJobStorage adaPotJobStorage) {
        super(storageWriter, stateService, eraService, properties, adaPotJobStorage);
    }

    @Override
    public String getTableName() {
        return "transaction_metadata";
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
                tm.id,
                tm.slot,
                tm.tx_hash,
                tm.label,
                tm.body,
                tm.cbor,
                tm.block,
                to_timestamp(tm.block_time) as block_time,
                tm.update_datetime
            FROM source_db.%s.transaction_metadata tm
            WHERE tm.slot >= %d
              AND tm.slot < %d
            ORDER BY tm.slot, tm.tx_hash
            """,
            schema,
            slotRange.startSlot(),
            slotRange.endSlot()
        );
    }
}


package com.bloxbean.cardano.yaci.store.analytics.exporter;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateService;
import com.bloxbean.cardano.yaci.store.analytics.writer.StorageWriter;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Exporter for transaction witnesses.
 *
 * Partitioning: DAILY (date=yyyy-MM-dd)
 * Source: transaction_witness table (joined with transaction for block_time)
 * Output: transaction_witness/date=2024-01-15/data.parquet
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class TransactionWitnessExporter extends AbstractTableExporter {

    public TransactionWitnessExporter(
            StorageWriter storageWriter,
            ExportStateService stateService,
            EraService eraService,
            AnalyticsStoreProperties properties) {
        super(storageWriter, stateService, eraService, properties);
    }

    @Override
    public String getTableName() {
        return "transaction_witness";
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
                tw.tx_hash,
                tw.idx,
                tw.pub_key,
                tw.signature,
                tw.pub_keyhash,
                tw.type,
                tw.additional_data,
                tw.slot,
                t.block_hash,
                t.block,
                t.epoch,
                to_timestamp(t.block_time) as block_time
            FROM source_db.%s.transaction_witness tw
            INNER JOIN source_db.%s.transaction t
                ON t.tx_hash = tw.tx_hash
            WHERE tw.slot >= %d
              AND tw.slot < %d
            ORDER BY tw.slot, tw.tx_hash, tw.idx
            """,
            schema, schema,
            slotRange.startSlot(),
            slotRange.endSlot()
        );
    }
}


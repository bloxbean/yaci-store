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
 * Exporter for stake withdrawals.
 *
 * Partitioning: DAILY (date=yyyy-MM-dd)
 * Source: withdrawal table
 * Output: withdrawal/date=2024-01-15/data.parquet
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class WithdrawalExporter extends AbstractTableExporter {

    public WithdrawalExporter(
            StorageWriter storageWriter,
            ExportStateService stateService,
            EraService eraService,
            AnalyticsStoreProperties properties,
            AdaPotJobStorage adaPotJobStorage) {
        super(storageWriter, stateService, eraService, properties, adaPotJobStorage);
    }

    @Override
    public String getTableName() {
        return "withdrawal";
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
                w.tx_hash,
                w.address,
                w.amount,
                w.epoch,
                w.slot,
                w.block,
                to_timestamp(w.block_time) as block_time
            FROM source_db.%s.withdrawal w
            WHERE w.slot >= %d
              AND w.slot < %d
            ORDER BY w.slot, w.tx_hash, w.address
            """,
            schema,
            slotRange.startSlot(),
            slotRange.endSlot()
        );
    }
}


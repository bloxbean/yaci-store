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
 * Exporter for per-address transaction amount entries.
 *
 * Partitioning: DAILY (date=yyyy-MM-dd)
 * Source: address_tx_amount table
 * Output: address_tx_amount/date=2024-01-15/data.parquet
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class AddressTxAmountExporter extends AbstractTableExporter {

    public AddressTxAmountExporter(
            StorageWriter storageWriter,
            ExportStateService stateService,
            EraService eraService,
            AnalyticsStoreProperties properties,
            AdaPotJobStorage adaPotJobStorage) {
        super(storageWriter, stateService, eraService, properties, adaPotJobStorage);
    }

    @Override
    public String getTableName() {
        return "address_tx_amount";
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
                ata.address,
                ata.unit,
                ata.tx_hash,
                ata.slot,
                ata.quantity,
                ata.addr_full,
                ata.stake_address,
                ata.block,
                ata.epoch,
                to_timestamp(ata.block_time) as block_time
            FROM source_db.%s.address_tx_amount ata
            WHERE ata.slot >= %d
              AND ata.slot < %d
            ORDER BY ata.slot, ata.address, ata.unit, ata.tx_hash
            """,
            schema,
            slotRange.startSlot(),
            slotRange.endSlot()
        );
    }
}


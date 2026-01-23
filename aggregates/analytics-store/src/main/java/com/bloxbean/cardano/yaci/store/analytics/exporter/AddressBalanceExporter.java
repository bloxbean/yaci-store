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
 * Exporter for address balance snapshots.
 *
 * Exports the latest address balance state for each (address, unit) combination
 * as of the partition date. This provides point-in-time balance snapshots.
 *
 * Partitioning: DAILY (date=yyyy-MM-dd)
 * Source: address_balance table
 * Output: address_balance/date=2024-01-15/data.parquet
 *
 * The query selects the latest balance update for each address+unit within the day,
 * enabling time-series analysis of address holdings.
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class AddressBalanceExporter extends AbstractTableExporter {

    public AddressBalanceExporter(
            StorageWriter storageWriter,
            ExportStateService stateService,
            EraService eraService,
            AnalyticsStoreProperties properties,
            AdaPotJobStorage adaPotJobStorage) {
        super(storageWriter, stateService, eraService, properties, adaPotJobStorage);
    }

    @Override
    public String getTableName() {
        return "address_balance";
    }

    @Override
    public PartitionStrategy getPartitionStrategy() {
        return PartitionStrategy.DAILY;
    }

    /**
     * Build SQL query for address balance at end of a specific date.
     *
     * The query selects the latest address balance state as of the given date.
     * Uses slot-based filtering for better performance (slot column is indexed).
     */
    @Override
    protected String buildQuery(PartitionValue partition, SlotRange slotRange) {
        String schema = getSourceSchema();
        return String.format("""
            SELECT
                ab.address,
                ab.quantity,
                ab.unit,
                to_timestamp(ab.block_time) as block_time,
                ab.block,
                ab.epoch,
                ab.slot,
                ab.addr_full
            FROM source_db.%s.address_balance ab
            INNER JOIN (
                SELECT
                    address,
                    unit,
                    MAX(slot) as max_slot
                FROM source_db.%s.address_balance
                WHERE slot >= %d
                  AND slot < %d
                GROUP BY address, unit
            ) latest
            ON ab.address = latest.address
               AND ab.unit = latest.unit
               AND ab.slot = latest.max_slot
            ORDER BY ab.address, ab.unit
            """,
            schema, schema,
            slotRange.startSlot(),
            slotRange.endSlot()
        );
    }
}

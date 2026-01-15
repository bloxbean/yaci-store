package com.bloxbean.cardano.yaci.store.analytics.exporter;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateService;
import com.bloxbean.cardano.yaci.store.analytics.writer.StorageWriter;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Exporter for unique addresses.
 *
 * This exporter tracks all unique addresses encountered on the blockchain.
 * The address table stores:
 * - Unique payment addresses (base58 or bech32)
 * - Full address for Byron-era addresses that don't fit
 * - Payment credential and stake address associations
 *
 * Partitioning: DAILY (date=yyyy-MM-dd)
 * Source: address table
 * Output: address/date=yyyy-MM-dd/data.parquet
 *
 * Note: Addresses are exported based on the slot when they were first seen.
 * This enables tracking of address discovery over time.
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class AddressExporter extends AbstractTableExporter {

    public AddressExporter(
            StorageWriter storageWriter,
            ExportStateService stateService,
            EraService eraService,
            AnalyticsStoreProperties properties) {
        super(storageWriter, stateService, eraService, properties);
    }

    @Override
    public String getTableName() {
        return "address";
    }

    @Override
    public PartitionStrategy getPartitionStrategy() {
        return PartitionStrategy.DAILY;
    }

    /**
     * Build SQL query for addresses discovered in the partition slot range.
     *
     * The query exports all addresses where the slot falls within the partition range.
     * This ensures each address is exported based on when it was first seen.
     */
    @Override
    protected String buildQuery(PartitionValue partition, SlotRange slotRange) {
        String schema = getSourceSchema();
        return String.format("""
            SELECT
                a.address,
                a.addr_full,
                a.payment_credential,
                a.stake_address,
                a.slot,
                a.update_datetime
            FROM source_db.%s.address a
            WHERE a.slot >= %d
              AND a.slot < %d
            ORDER BY a.slot, a.address
            """,
            schema,
            slotRange.startSlot(),
            slotRange.endSlot()
        );
    }
}

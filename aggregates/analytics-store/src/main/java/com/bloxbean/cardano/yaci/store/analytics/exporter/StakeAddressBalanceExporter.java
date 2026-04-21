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
 * Exporter for stake address balance snapshots.
 *
 * Exports the latest stake address balance state for each stake address
 * as of the partition date. This provides point-in-time stake balance snapshots.
 *
 * Partitioning: DAILY (date=yyyy-MM-dd)
 * Source: stake_address_balance table
 * Output: stake_address_balance/date=2024-01-15/data.parquet
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class StakeAddressBalanceExporter extends AbstractTableExporter {

    public StakeAddressBalanceExporter(
            StorageWriter storageWriter,
            ExportStateService stateService,
            EraService eraService,
            AnalyticsStoreProperties properties,
            AdaPotJobStorage adaPotJobStorage) {
        super(storageWriter, stateService, eraService, properties, adaPotJobStorage);
    }

    @Override
    public String getTableName() {
        return "stake_address_balance";
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
                    address,
                    quantity,
                    to_timestamp(COALESCE(block_time, 0)) as block_time,
                    block,
                    epoch,
                    slot
                FROM (
                    SELECT
                        address,
                        quantity,
                        block_time,
                        block,
                        epoch,
                        slot,
                        ROW_NUMBER() OVER (PARTITION BY address ORDER BY slot DESC) as rn
                    FROM %s.stake_address_balance
                    WHERE slot >= %d
                      AND slot < %d
                ) ranked
                WHERE rn = 1
                ORDER BY slot
            ')
            """,
            schema,
            slotRange.startSlot(),
            slotRange.endSlot()
        );
    }
}


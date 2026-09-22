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
 * Lossless stake balance history for snapshot restore, including changes within one day.
 * Kept separate from the daily downsample used by analytics: filtering that downsample at an
 * epoch boundary can discard the last balance before the boundary, and cannot support rollback.
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics",
        name = "enabled", havingValue = "true")
public class StakeAddressBalanceSnapshotExporter extends AbstractTableExporter {

    public StakeAddressBalanceSnapshotExporter(
            StorageWriter storageWriter,
            ExportStateService stateService,
            EraService eraService,
            AnalyticsStoreProperties properties,
            AdaPotJobStorage adaPotJobStorage) {
        super(storageWriter, stateService, eraService, properties, adaPotJobStorage);
    }

    @Override
    public String getTableName() {
        return "stake_address_balance_snapshot";
    }

    @Override
    public PartitionStrategy getPartitionStrategy() {
        return PartitionStrategy.DAILY;
    }

    @Override
    public String getFederationBoundaryColumn() {
        // This relation has no same-named live PostgreSQL table.
        return null;
    }

    @Override
    protected String buildQuery(PartitionValue partition, SlotRange slotRange) {
        return String.format("""
            SELECT * FROM postgres_query('source_db', '
                SELECT address, quantity,
                       to_timestamp(COALESCE(block_time, 0)) AS block_time,
                       block, epoch, slot
                FROM %s.stake_address_balance
                WHERE slot >= %d AND slot < %d
                ORDER BY slot
            ')
            """, getSourceSchema(), slotRange.startSlot(), slotRange.endSlot());
    }
}

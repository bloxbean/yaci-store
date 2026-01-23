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
 * Exporter for native asset mint/burn events.
 *
 * Partitioning: DAILY (date=yyyy-MM-dd)
 * Source: assets table
 * Output: assets/date=2024-01-15/data.parquet
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class AssetsExporter extends AbstractTableExporter {

    public AssetsExporter(
            StorageWriter storageWriter,
            ExportStateService stateService,
            EraService eraService,
            AnalyticsStoreProperties properties,
            AdaPotJobStorage adaPotJobStorage) {
        super(storageWriter, stateService, eraService, properties, adaPotJobStorage);
    }

    @Override
    public String getTableName() {
        return "assets";
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
                a.id,
                a.slot,
                a.tx_hash,
                a.policy,
                a.asset_name,
                a.unit,
                a.fingerprint,
                a.quantity,
                a.mint_type,
                a.block,
                to_timestamp(a.block_time) as block_time
            FROM source_db.%s.assets a
            WHERE a.slot >= %d
              AND a.slot < %d
            ORDER BY a.slot, a.tx_hash, a.unit
            """,
            schema,
            slotRange.startSlot(),
            slotRange.endSlot()
        );
    }
}


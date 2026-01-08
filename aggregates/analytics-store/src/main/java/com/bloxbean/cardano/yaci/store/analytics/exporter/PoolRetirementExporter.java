package com.bloxbean.cardano.yaci.store.analytics.exporter;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateService;
import com.bloxbean.cardano.yaci.store.analytics.writer.StorageWriter;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Exporter for pool retirements.
 *
 * Partitioning: DAILY (date=yyyy-MM-dd)
 * Source: pool_retirement table
 * Output: pool_retirement/date=yyyy-MM-dd/data.parquet
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class PoolRetirementExporter extends AbstractTableExporter {

    public PoolRetirementExporter(
            StorageWriter storageWriter,
            ExportStateService stateService,
            EraService eraService,
            AnalyticsStoreProperties properties) {
        super(storageWriter, stateService, eraService, properties);
    }

    @Override
    public String getTableName() {
        return "pool_retirement";
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
                pr.tx_hash,
                pr.cert_index,
                pr.tx_index,
                pr.slot,
                pr.pool_id,
                pr.retirement_epoch,
                pr.epoch,
                pr.block_hash,
                to_timestamp(pr.block_time) as block_time
            FROM source_db.%s.pool_retirement pr
            WHERE pr.slot >= %d
              AND pr.slot < %d
            ORDER BY pr.slot, pr.tx_hash, pr.cert_index
            """,
            schema,
            slotRange.startSlot(),
            slotRange.endSlot()
        );
    }
}

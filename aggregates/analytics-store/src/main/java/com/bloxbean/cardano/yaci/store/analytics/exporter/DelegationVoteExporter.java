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
 * Exporter for governance delegation vote certificates.
 *
 * Partitioning: DAILY (date=yyyy-MM-dd)
 * Source: delegation_vote table
 * Output: delegation_vote/date=2024-01-15/data.parquet
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class DelegationVoteExporter extends AbstractTableExporter {

    public DelegationVoteExporter(
            StorageWriter storageWriter,
            ExportStateService stateService,
            EraService eraService,
            AnalyticsStoreProperties properties,
            AdaPotJobStorage adaPotJobStorage) {
        super(storageWriter, stateService, eraService, properties, adaPotJobStorage);
    }

    @Override
    public String getTableName() {
        return "delegation_vote";
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
                dv.tx_hash,
                dv.cert_index,
                dv.tx_index,
                dv.address,
                dv.drep_hash,
                dv.drep_id,
                dv.drep_type,
                dv.epoch,
                dv.credential,
                dv.cred_type,
                dv.slot,
                dv.block,
                to_timestamp(dv.block_time) as block_time
            FROM source_db.%s.delegation_vote dv
            WHERE dv.slot >= %d
              AND dv.slot < %d
            ORDER BY dv.slot, dv.tx_index, dv.cert_index
            """,
            schema,
            slotRange.startSlot(),
            slotRange.endSlot()
        );
    }
}


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
 * Exporter for governance voting procedures.
 *
 * Partitioning: DAILY (date=yyyy-MM-dd)
 * Source: voting_procedure table
 * Output: voting_procedure/date=yyyy-MM-dd/data.parquet
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class VotingProcedureExporter extends AbstractTableExporter {

    public VotingProcedureExporter(
            StorageWriter storageWriter,
            ExportStateService stateService,
            EraService eraService,
            AnalyticsStoreProperties properties,
            AdaPotJobStorage adaPotJobStorage) {
        super(storageWriter, stateService, eraService, properties, adaPotJobStorage);
    }

    @Override
    public String getTableName() {
        return "voting_procedure";
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
                vp.tx_hash,
                vp.idx,
                vp.tx_index,
                vp.slot,
                vp.voter_type,
                vp.voter_hash,
                vp.gov_action_tx_hash,
                vp.gov_action_index,
                vp.vote,
                vp.anchor_url,
                vp.anchor_hash,
                vp.epoch,
                vp.block,
                to_timestamp(vp.block_time) as block_time
            FROM source_db.%s.voting_procedure vp
            WHERE vp.slot >= %d
              AND vp.slot < %d
            ORDER BY vp.slot, vp.tx_hash, vp.idx
            """,
            schema,
            slotRange.startSlot(),
            slotRange.endSlot()
        );
    }
}

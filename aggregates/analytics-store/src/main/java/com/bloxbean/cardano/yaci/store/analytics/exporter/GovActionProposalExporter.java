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
 * Exporter for governance action proposals.
 *
 * Partitioning: DAILY (date=yyyy-MM-dd)
 * Source: gov_action_proposal table
 * Output: gov_action_proposal/date=yyyy-MM-dd/data.parquet
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class GovActionProposalExporter extends AbstractTableExporter {

    public GovActionProposalExporter(
            StorageWriter storageWriter,
            ExportStateService stateService,
            EraService eraService,
            AnalyticsStoreProperties properties,
            AdaPotJobStorage adaPotJobStorage) {
        super(storageWriter, stateService, eraService, properties, adaPotJobStorage);
    }

    @Override
    public String getTableName() {
        return "gov_action_proposal";
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
                gap.tx_hash,
                gap.idx,
                gap.tx_index,
                gap.slot,
                gap.deposit,
                gap.return_address,
                gap.type,
                gap.details::text as details,
                gap.anchor_url,
                gap.anchor_hash,
                gap.epoch,
                gap.block,
                to_timestamp(gap.block_time) as block_time
            FROM source_db.%s.gov_action_proposal gap
            WHERE gap.slot >= %d
              AND gap.slot < %d
            ORDER BY gap.slot, gap.tx_hash, gap.idx
            """,
            schema,
            slotRange.startSlot(),
            slotRange.endSlot()
        );
    }
}

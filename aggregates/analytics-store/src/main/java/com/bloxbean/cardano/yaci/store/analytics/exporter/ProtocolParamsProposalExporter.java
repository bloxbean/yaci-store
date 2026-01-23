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
 * Exporter for protocol parameter proposals.
 *
 * Partitioning: DAILY (date=yyyy-MM-dd)
 * Source: protocol_params_proposal table
 * Output: protocol_params_proposal/date=yyyy-MM-dd/data.parquet
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class ProtocolParamsProposalExporter extends AbstractTableExporter {

    public ProtocolParamsProposalExporter(
            StorageWriter storageWriter,
            ExportStateService stateService,
            EraService eraService,
            AnalyticsStoreProperties properties,
            AdaPotJobStorage adaPotJobStorage) {
        super(storageWriter, stateService, eraService, properties, adaPotJobStorage);
    }

    @Override
    public String getTableName() {
        return "protocol_params_proposal";
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
                ppp.tx_hash,
                ppp.key_hash,
                ppp.params,
                ppp.target_epoch,
                ppp.epoch,
                ppp.slot,
                ppp.era,
                ppp.block,
                to_timestamp(ppp.block_time) as block_time
            FROM source_db.%s.protocol_params_proposal ppp
            WHERE ppp.slot >= %d
              AND ppp.slot < %d
            ORDER BY ppp.slot, ppp.tx_hash
            """,
            schema,
            slotRange.startSlot(),
            slotRange.endSlot()
        );
    }
}

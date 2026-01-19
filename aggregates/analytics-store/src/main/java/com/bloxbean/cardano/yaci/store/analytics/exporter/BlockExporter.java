package com.bloxbean.cardano.yaci.store.analytics.exporter;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateService;
import com.bloxbean.cardano.yaci.store.analytics.writer.StorageWriter;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Exporter for blocks.
 *
 * Partitioning: DAILY (date=yyyy-MM-dd)
 * Source: block table
 * Output: block/date=2024-01-15/data.parquet
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class BlockExporter extends AbstractTableExporter {

    public BlockExporter(
            StorageWriter storageWriter,
            ExportStateService stateService,
            EraService eraService,
            AnalyticsStoreProperties properties) {
        super(storageWriter, stateService, eraService, properties);
    }

    @Override
    public String getTableName() {
        return "block";
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
                b.hash,
                b.number,
                b.body_hash,
                b.body_size,
                b.epoch,
                CAST(b.total_output AS DECIMAL(38,0)) as total_output,
                b.total_fees,
                to_timestamp(b.block_time) as block_time,
                b.era,
                b.issuer_vkey,
                b.leader_vrf,
                b.nonce_vrf,
                b.prev_hash,
                b.protocol_version,
                b.slot,
                b.vrf_result,
                b.vrf_vkey,
                b.no_of_txs,
                b.slot_leader,
                b.epoch_slot,
                b.op_cert_hot_vkey,
                b.op_cert_seq_number,
                b.op_cert_kes_period,
                b.op_cert_sigma
            FROM source_db.%s.block b
            WHERE b.slot >= %d
              AND b.slot < %d
            ORDER BY b.slot, b.hash
            """,
            schema,
            slotRange.startSlot(),
            slotRange.endSlot()
        );
    }
}


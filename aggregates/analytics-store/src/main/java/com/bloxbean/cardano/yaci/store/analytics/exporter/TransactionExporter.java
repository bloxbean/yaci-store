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
 * Exporter for transaction metadata.
 *
 * Exports transaction-level data including:
 * - Transaction hash, block info, timing
 * - Fee, size, total output
 * - Invalid flag, script size
 * - JSONB arrays for inputs/outputs/reference inputs/collateral
 *
 * Partitioning: DAILY (date=yyyy-MM-dd)
 * Source: transaction table
 * Output: transactions/date=2024-01-15/data.parquet
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class TransactionExporter extends AbstractTableExporter {

    public TransactionExporter(
            StorageWriter storageWriter,
            ExportStateService stateService,
            EraService eraService,
            AnalyticsStoreProperties properties,
            AdaPotJobStorage adaPotJobStorage) {
        super(storageWriter, stateService, eraService, properties, adaPotJobStorage);
    }

    @Override
    public String getTableName() {
        return "transaction";
    }

    @Override
    public PartitionStrategy getPartitionStrategy() {
        return PartitionStrategy.DAILY;
    }

    /**
     * Build SQL query for transaction metadata.
     *
     * Exports all transaction fields including JSONB arrays for inputs/outputs.
     * Filters by slot range (indexed column for performance).
     */
    @Override
    protected String buildQuery(PartitionValue partition, SlotRange slotRange) {
        String schema = getSourceSchema();
        return String.format("""
            SELECT
                t.tx_hash,
                t.block_hash,
                t.block,
                t.slot,
                t.epoch,
                to_timestamp(t.block_time) as block_time,
                t.tx_index,
                t.fee,
                t.invalid,
                t.network_id,
                t.auxiliary_datahash,
                t.script_datahash,
                t.total_collateral,
                t.ttl,
                t.validity_interval_start,
                t.treasury_donation,
                t.inputs,
                t.outputs,
                t.reference_inputs,
                t.collateral_inputs,
                t.collateral_return,
                t.collateral_return_json,
                t.required_signers
            FROM source_db.%s.transaction t
            WHERE t.slot >= %d
              AND t.slot < %d
            ORDER BY t.slot, t.tx_hash
            """,
            schema,
            slotRange.startSlot(),
            slotRange.endSlot()
        );
    }
}

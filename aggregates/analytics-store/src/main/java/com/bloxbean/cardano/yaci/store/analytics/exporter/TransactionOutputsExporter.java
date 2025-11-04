package com.bloxbean.cardano.yaci.store.analytics.exporter;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateService;
import com.bloxbean.cardano.yaci.store.analytics.writer.StorageWriter;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
/**
 * Exporter for transaction outputs with flattened JSONB amounts.
 *
 * This exporter flattens the JSONB amounts array in address_utxo table:
 * - 1 UTXO with 3 assets = 3 rows in Parquet (one row per asset)
 * - Enables efficient columnar analytics without JSONB parsing overhead
 *
 * Partitioning: DAILY (date=yyyy-MM-dd)
 * Source: address_utxo_flattened VIEW
 * Output: transaction_outputs/date=2024-01-15/data.parquet
 *
 * The view uses CROSS JOIN LATERAL jsonb_array_elements() to flatten amounts
 * on-demand, avoiding materialized view maintenance overhead.
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class TransactionOutputsExporter extends AbstractTableExporter {

    public TransactionOutputsExporter(
            StorageWriter storageWriter,
            ExportStateService stateService,
            EraService eraService,
            AnalyticsStoreProperties properties) {
        super(storageWriter, stateService, eraService, properties);
    }

    @Override
    public String getTableName() {
        return "address_utxo";
    }

    @Override
    public PartitionStrategy getPartitionStrategy() {
        return PartitionStrategy.DAILY;
    }

    /**
     * Build SQL query for transaction outputs with flattened amounts.
     *
     * Queries the address_utxo_flattened VIEW which uses CROSS JOIN LATERAL
     * jsonb_array_elements() to flatten the amounts array on-demand.
     * PostgreSQL performs flattening only for the requested slot range.
     *
     * Result: One row per asset per output (row explosion for multi-asset UTXOs)
     * Benefits:
     * - Proper Parquet columns with statistics (enables fast queries on asset_unit, policy_id)
     * - Dictionary encoding on repeated values
     * - 10-100x faster queries compared to JSON text approach
     */
    @Override
    protected String buildQuery(PartitionValue partition, SlotRange slotRange) {
        String schema = getSourceSchema();
        return String.format("""
            SELECT
                tx_hash,
                output_index,
                asset_unit,
                policy_id,
                asset_name,
                CAST(quantity AS DECIMAL(38,0)) as quantity,
                owner_addr,
                owner_stake_addr,
                owner_payment_credential,
                owner_stake_credential,
                inline_datum,
                data_hash,
                script_ref,
                reference_script_hash,
                is_collateral_return,
                epoch,
                slot,
                block_hash,
                to_timestamp(block_time) as block_time
            FROM source_db.%s.address_utxo_flattened
            WHERE slot >= %d
              AND slot < %d
            ORDER BY slot, tx_hash, output_index
            """,
            schema,
            slotRange.startSlot(),
            slotRange.endSlot()
        );
    }
}

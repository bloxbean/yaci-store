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
 * Exporter for pool registrations.
 *
 * Partitioning: DAILY (date=yyyy-MM-dd)
 * Source: pool_registration table
 * Output: pool_registration/date=yyyy-MM-dd/data.parquet
 */
/*
 * margin_numerator and margin_denominator are exported alongside the computed margin because the
 * AdaPot reward calculation reads the exact rational (PoolDetails.getMargin) rather than the float:
 * double arithmetic drifts from Haskell ledger math. Without them a database restored from a
 * snapshot computes every pool's operator/member split at margin zero.
 *
 * Note for operators: this widens the pool_registration DuckLake relation. The writer creates the
 * table once by CTAS and thereafter inserts into it, so an existing export must have its
 * pool_registration table and export state cleared before the new columns appear.
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class PoolRegistrationExporter extends AbstractTableExporter {

    public PoolRegistrationExporter(
            StorageWriter storageWriter,
            ExportStateService stateService,
            EraService eraService,
            AnalyticsStoreProperties properties,
            AdaPotJobStorage adaPotJobStorage) {
        super(storageWriter, stateService, eraService, properties, adaPotJobStorage);
    }

    @Override
    public String getTableName() {
        return "pool_registration";
    }

    @Override
    public PartitionStrategy getPartitionStrategy() {
        return PartitionStrategy.DAILY;
    }

    @Override
    protected String buildQuery(PartitionValue partition, SlotRange slotRange) {
        String schema = getSourceSchema();
        return String.format("""
            SELECT * FROM postgres_query('source_db', '
                SELECT
                    pr.tx_hash,
                    pr.cert_index,
                    pr.tx_index,
                    pr.slot,
                    pr.pool_id,
                    pr.vrf_key as vrf_key_hash,
                    pr.pledge,
                    pr.cost,
                    pr.margin,
                    pr.margin_numerator,
                    pr.margin_denominator,
                    pr.reward_account,
                    pr.pool_owners::text as pool_owners,
                    pr.relays::text as relays,
                    pr.metadata_url,
                    pr.metadata_hash,
                    pr.epoch,
                    pr.block_hash,
                    to_timestamp(COALESCE(pr.block_time, 0)) as block_time
                FROM %s.pool_registration pr
                WHERE pr.slot >= %d
                  AND pr.slot < %d
                ORDER BY pr.slot
            ')
            """,
            schema,
            slotRange.startSlot(),
            slotRange.endSlot()
        );
    }
}

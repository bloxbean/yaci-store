package com.bloxbean.cardano.yaci.store.analytics.exporter;

import com.bloxbean.cardano.yaci.store.adapot.job.storage.AdaPotJobStorage;
import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateService;
import com.bloxbean.cardano.yaci.store.analytics.writer.StorageWriter;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

/**
 * Exporter for CIP-68 reference NFT metadata written by the assets extension.
 *
 * Only registered when the assets extension and its CIP-68 processor are enabled, because the
 * table is only populated then; exporting it otherwise would record empty history.
 *
 * Partitioning: DAILY (date=yyyy-MM-dd)
 * Source: cip68_metadata table
 * Output: cip68_metadata/date=yyyy-MM-dd/data.parquet
 */
@Service
@Slf4j
@ConditionalOnExpression("${yaci.store.analytics.enabled:false} and ${store.assets.ext.enabled:false}"
        + " and ${store.assets.ext.cip68.enabled:true}")
public class Cip68MetadataExporter extends AbstractTableExporter {

    public Cip68MetadataExporter(
            StorageWriter storageWriter,
            ExportStateService stateService,
            EraService eraService,
            AnalyticsStoreProperties properties,
            AdaPotJobStorage adaPotJobStorage) {
        super(storageWriter, stateService, eraService, properties, adaPotJobStorage);
    }

    @Override
    public String getTableName() {
        return "cip68_metadata";
    }

    @Override
    public PartitionStrategy getPartitionStrategy() {
        return PartitionStrategy.DAILY;
    }

    @Override
    public String getPartitionColumn() {
        return "block_date";
    }

    @Override
    protected String buildQuery(PartitionValue partition, SlotRange slotRange) {
        String schema = getSourceSchema();
        String dateStr = ((PartitionValue.DatePartition) partition).date().toString();
        return String.format("""
            SELECT * FROM postgres_query('source_db', '
                SELECT
                    c.policy_id,
                    c.asset_name,
                    c.slot,
                    c.tx_hash,
                    c.tx_index,
                    c.label,
                    c.name,
                    c.description,
                    c.ticker,
                    c.url,
                    c.decimals,
                    c.logo,
                    c.image,
                    c.media_type,
                    c.version,
                    c.datum,
                    c.properties::text as properties,
                    c.last_synced_at,
                    CAST(''%s'' AS DATE) as block_date
                FROM %s.cip68_metadata c
                WHERE c.slot >= %d
                  AND c.slot < %d
                ORDER BY c.slot, c.tx_hash, c.policy_id, c.asset_name
            ')
            """,
            dateStr, schema,
            slotRange.startSlot(),
            slotRange.endSlot()
        );
    }
}

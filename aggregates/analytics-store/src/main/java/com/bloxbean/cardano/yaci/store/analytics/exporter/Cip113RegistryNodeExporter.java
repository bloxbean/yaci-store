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
 * Exporter for the CIP-113 programmable-token registry nodes written by the assets extension.
 *
 * Registered whenever the assets extension is enabled, whatever {@code store.assets.ext.cip113.enabled}
 * says: the table exists with the extension's schema, and it is empty when CIP-113 is off or no registry is
 * known for the network. Exporting it then keeps the relation in the catalog, so a snapshot of such a
 * source carries the (empty) table instead of failing on a missing relation.
 *
 * Partitioning: DAILY (date=yyyy-MM-dd)
 * Source: cip113_registry_node table
 * Output: cip113_registry_node/date=yyyy-MM-dd/data.parquet
 */
@Service
@Slf4j
@ConditionalOnExpression("${yaci.store.analytics.enabled:false} and ${store.assets.ext.enabled:false}")
public class Cip113RegistryNodeExporter extends AbstractTableExporter {

    public Cip113RegistryNodeExporter(
            StorageWriter storageWriter,
            ExportStateService stateService,
            EraService eraService,
            AnalyticsStoreProperties properties,
            AdaPotJobStorage adaPotJobStorage) {
        super(storageWriter, stateService, eraService, properties, adaPotJobStorage);
    }

    @Override
    public String getTableName() {
        return "cip113_registry_node";
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
                    n.key,
                    n.registry_policy_id,
                    n.slot,
                    n.tx_hash,
                    n.tx_index,
                    n.output_index,
                    n.next,
                    n.minting_logic_script,
                    n.transfer_logic_script,
                    n.third_party_transfer_logic_script,
                    n.unfracking_logic_script,
                    n.global_state_policy_id,
                    n.datum,
                    n.last_synced_at,
                    CAST(''%s'' AS DATE) as block_date
                FROM %s.cip113_registry_node n
                WHERE n.slot >= %d
                  AND n.slot < %d
                ORDER BY n.slot, n.tx_hash, n.key, n.registry_policy_id
            ')
            """,
            dateStr, schema,
            slotRange.startSlot(),
            slotRange.endSlot()
        );
    }
}

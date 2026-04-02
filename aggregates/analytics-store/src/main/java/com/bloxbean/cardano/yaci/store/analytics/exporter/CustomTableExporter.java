package com.bloxbean.cardano.yaci.store.analytics.exporter;

import com.bloxbean.cardano.yaci.store.adapot.job.storage.AdaPotJobStorage;
import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties.CustomExporterConfig;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateService;
import com.bloxbean.cardano.yaci.store.analytics.writer.StorageWriter;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import lombok.extern.slf4j.Slf4j;

/**
 * A table exporter driven by user-defined SQL query templates in YAML configuration.
 *
 * Unlike built-in exporters (which are {@code @Service} beans), instances of this class
 * are created programmatically by {@link com.bloxbean.cardano.yaci.store.analytics.config.CustomExporterRegistrar}
 * and registered into the {@link TableExporterRegistry}.
 *
 * Query templates support the following placeholders:
 * <ul>
 *   <li>{@code {source}} — replaced with the schema name (e.g., {@code mainnet})</li>
 *   <li>{@code {start_slot}} — slot range start (inclusive)</li>
 *   <li>{@code {end_slot}} — slot range end (exclusive)</li>
 *   <li>{@code {epoch}} — epoch number (EPOCH strategy only)</li>
 * </ul>
 *
 * The resolved query is automatically wrapped in {@code postgres_query('source_db', '...')}
 * to ensure DuckDB pushes filtering down to PostgreSQL instead of performing full table scans.
 */
@Slf4j
public class CustomTableExporter extends AbstractTableExporter {

    private final String tableName;
    private final PartitionStrategy partitionStrategy;
    private final String partitionColumn;
    private final boolean dependsOnAdapotJob;
    private final String queryTemplate;

    public CustomTableExporter(
            CustomExporterConfig config,
            StorageWriter storageWriter,
            ExportStateService stateService,
            EraService eraService,
            AnalyticsStoreProperties properties,
            AdaPotJobStorage adaPotJobStorage) {
        super(storageWriter, stateService, eraService, properties, adaPotJobStorage);
        this.tableName = config.getName();
        this.partitionStrategy = PartitionStrategy.valueOf(config.getPartitionStrategy().toUpperCase());
        this.partitionColumn = config.getPartitionColumn();
        this.dependsOnAdapotJob = config.isDependsOnAdapotJob();
        this.queryTemplate = config.getQuery();
    }

    @Override
    public String getTableName() {
        return tableName;
    }

    @Override
    public PartitionStrategy getPartitionStrategy() {
        return partitionStrategy;
    }

    @Override
    public String getPartitionColumn() {
        return partitionColumn;
    }

    @Override
    public boolean preExportValidation(PartitionValue partition) {
        if (!dependsOnAdapotJob) return true;
        if (!(partition instanceof PartitionValue.EpochPartition ep)) return true;
        return isRewardCalcAdaPotJobCompleted(ep.epoch());
    }

    @Override
    protected String buildQuery(PartitionValue partition, SlotRange slotRange) {
        String schema = getSourceSchema();
        String resolvedQuery = queryTemplate
                .replace("{source}", schema)
                .replace("{start_slot}", String.valueOf(slotRange.startSlot()))
                .replace("{end_slot}", String.valueOf(slotRange.endSlot()));

        if (partition instanceof PartitionValue.EpochPartition ep) {
            resolvedQuery = resolvedQuery.replace("{epoch}", String.valueOf(ep.epoch()));
        }

        // Escape single quotes for postgres_query wrapper
        resolvedQuery = resolvedQuery.replace("'", "''");
        return String.format("SELECT * FROM postgres_query('source_db', '%s')", resolvedQuery);
    }
}

package com.bloxbean.cardano.yaci.store.analytics.exporter;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.adapot.job.storage.AdaPotJobStorage;
import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateService;
import com.bloxbean.cardano.yaci.store.analytics.writer.StorageWriter;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Exporter for epoch parameters.
 *
 * Partitioning: EPOCH (epoch=N)
 * Source: epoch_param table
 * Output: epoch_param/epoch=N/data.parquet
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class EpochParamExporter extends AbstractTableExporter {

    public EpochParamExporter(
            StorageWriter storageWriter,
            ExportStateService stateService,
            EraService eraService,
            AnalyticsStoreProperties properties,
            AdaPotJobStorage adaPotJobStorage) {
        super(storageWriter, stateService, eraService, properties, adaPotJobStorage);
    }

    @Override
    public String getTableName() {
        return "epoch_param";
    }

    @Override
    public boolean includesByronEpochs() {
        return true;
    }

    @Override
    public PartitionStrategy getPartitionStrategy() {
        return PartitionStrategy.EPOCH;
    }

    /**
     * Like the other epoch tables, an epoch is exported once its AdaPot job has run. From Conway on the
     * job's pre-processing is what writes the epoch's parameters; before Conway they are written at the
     * epoch transition, which the job always follows. Byron epochs have no job and pass. Without the
     * AdaPot module Conway parameters are never written, so only earlier epochs are exported.
     */
    @Override
    public boolean preExportValidation(PartitionValue partition) {
        int epoch = ((PartitionValue.EpochPartition) partition).epoch();
        if (adaPotJobStorage instanceof NoOpAdaPotJobStorage) {
            return eraService.getEraForEpoch(epoch).getValue() < Era.Conway.getValue();
        }
        return isRewardCalcAdaPotJobCompleted(epoch);
    }

    @Override
    protected String buildQuery(PartitionValue partition, SlotRange slotRange) {
        String schema = getSourceSchema();
        // Epoch params are usually one row per epoch.
        // We filter by epoch directly instead of slot range for correctness.
        
        int epoch = ((PartitionValue.EpochPartition) partition).epoch();
        
        return String.format("""
            SELECT * FROM postgres_query('source_db', '
                SELECT
                    ep.epoch,
                    ep.params,
                    ep.cost_model_hash,
                    ep.slot,
                    ep.block,
                    to_timestamp(COALESCE(ep.block_time, 0)) as block_time
                FROM %s.epoch_param ep
                WHERE ep.epoch = %d
            ')
            """,
            schema,
            epoch
        );
    }
}

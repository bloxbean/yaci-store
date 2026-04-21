package com.bloxbean.cardano.yaci.store.analytics.exporter;

import com.bloxbean.cardano.yaci.store.adapot.job.storage.AdaPotJobStorage;
import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateService;
import com.bloxbean.cardano.yaci.store.analytics.writer.StorageWriter;
import com.bloxbean.cardano.yaci.store.blocks.storage.BlockStorageReader;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Exporter for Move Instantaneous Rewards (MIR).
 *
 * Partitioning: EPOCH (epoch=N)
 * Source: mir table
 * Output: mir/epoch=N/data.parquet
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class MoveInstantaneousRewardExporter extends AbstractTableExporter {

    private final BlockStorageReader blockStorageReader;

    public MoveInstantaneousRewardExporter(
            StorageWriter storageWriter,
            ExportStateService stateService,
            EraService eraService,
            AnalyticsStoreProperties properties,
            AdaPotJobStorage adaPotJobStorage,
            BlockStorageReader blockStorageReader) {
        super(storageWriter, stateService, eraService, properties, adaPotJobStorage);
        this.blockStorageReader = blockStorageReader;
    }

    @Override
    public boolean preExportValidation(PartitionValue partition) {
        int epoch = ((PartitionValue.EpochPartition) partition).epoch();
        int currentEpoch = blockStorageReader.findRecentBlock()
                .map(com.bloxbean.cardano.yaci.store.blocks.domain.Block::getEpochNumber)
                .orElse(Integer.MAX_VALUE);
        return epoch < currentEpoch;
    }

    @Override
    public String getTableName() {
        return "mir";
    }

    @Override
    public PartitionStrategy getPartitionStrategy() {
        return PartitionStrategy.EPOCH;
    }

    @Override
    protected String buildQuery(PartitionValue partition, SlotRange slotRange) {
        String schema = getSourceSchema();
        int epoch = ((PartitionValue.EpochPartition) partition).epoch();

        return String.format("""
            SELECT * FROM postgres_query('source_db', '
                SELECT
                    m.tx_hash,
                    m.cert_index,
                    m.pot,
                    m.credential,
                    m.address,
                    m.amount,
                    m.epoch,
                    m.slot,
                    m.block_hash,
                    to_timestamp(COALESCE(m.block_time, 0)) as block_time
                FROM %s.mir m
                WHERE m.epoch = %d
                ORDER BY m.slot
            ')
            """,
            schema,
            epoch
        );
    }
}

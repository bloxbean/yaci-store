package com.bloxbean.cardano.yaci.store.analytics.exporter;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateService;
import com.bloxbean.cardano.yaci.store.analytics.writer.StorageWriter;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Exporter for stake registrations.
 *
 * Partitioning: DAILY (date=yyyy-MM-dd)
 * Source: stake_registration table
 * Output: stake_registration/date=2024-01-15/data.parquet
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class StakeRegistrationExporter extends AbstractTableExporter {

    public StakeRegistrationExporter(
            StorageWriter storageWriter,
            ExportStateService stateService,
            EraService eraService,
            AnalyticsStoreProperties properties) {
        super(storageWriter, stateService, eraService, properties);
    }

    @Override
    public String getTableName() {
        return "stake_registration";
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
                sr.tx_hash,
                sr.cert_index,
                sr.tx_index,
                sr.credential,
                sr.cred_type,
                sr.type,
                sr.address,
                sr.epoch,
                sr.slot,
                sr.block_hash,
                sr.block,
                to_timestamp(sr.block_time) as block_time
            FROM source_db.%s.stake_registration sr
            WHERE sr.slot >= %d
              AND sr.slot < %d
            ORDER BY sr.slot, sr.tx_index, sr.cert_index
            """,
            schema,
            slotRange.startSlot(),
            slotRange.endSlot()
        );
    }
}


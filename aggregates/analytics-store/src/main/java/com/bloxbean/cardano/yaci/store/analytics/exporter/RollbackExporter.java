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

import java.math.BigDecimal;

/**
 * Exporter for blockchain rollback events.
 *
 * This exporter tracks rollback events that occurred during chain synchronization.
 * Rollbacks happen when the chain reorganizes due to competing forks.
 *
 * Partitioning: DAILY (date=yyyy-MM-dd of current_slot)
 * Source: rollback table
 * Output: rollback/date=yyyy-MM-dd/data.parquet
 *
 * Use cases:
 * - Chain stability analysis
 * - Understanding reorganization patterns
 * - Debugging sync issues
 * - Monitoring chain health
 *
 * Note: This table typically has low volume but is important for understanding
 * chain dynamics and potential data consistency issues.
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class RollbackExporter extends AbstractTableExporter {

    public RollbackExporter(
            StorageWriter storageWriter,
            ExportStateService stateService,
            EraService eraService,
            AnalyticsStoreProperties properties,
            AdaPotJobStorage adaPotJobStorage) {
        super(storageWriter, stateService, eraService, properties, adaPotJobStorage);
    }

    @Override
    public String getTableName() {
        return "rollback";
    }

    @Override
    public PartitionStrategy getPartitionStrategy() {
        return PartitionStrategy.DAILY;
    }

    /**
     * Build SQL query for rollback events in the slot range.
     *
     * <p>A rollback row is filed under the day of its {@code current_slot} (the tip when the
     * rollback happened), like every other daily table files a row under the day of its slot.
     * Its {@code block_time} is the start time of that slot, computed from the era
     * configuration the partition slot ranges are computed from, so it always falls on the
     * partition's day. It cannot be read from the block table: the block at
     * {@code current_slot} is the one the rollback removed, so a join with {@code block} drops
     * every real rollback.</p>
     */
    @Override
    protected String buildQuery(PartitionValue partition, SlotRange slotRange) {
        String schema = getSourceSchema();
        return String.format("""
            SELECT * FROM postgres_query('source_db', '
                SELECT
                    r.id,
                    r.rollback_to_block_hash,
                    r.rollback_to_slot,
                    r.current_block_hash,
                    r.current_slot,
                    r.current_block,
                    to_timestamp(%s) as block_time
                FROM %s.rollback r
                WHERE r.current_slot >= %d
                  AND r.current_slot < %d
                ORDER BY r.current_slot, r.id
            ')
            """,
            slotTimeSql("r.current_slot", eraService),
            schema,
            slotRange.startSlot(),
            slotRange.endSlot()
        );
    }

    /**
     * SQL expression for the start time (epoch seconds) of the slot in {@code slotColumn}: the
     * same arithmetic as {@link EraService#blockTime(Era, long)}, with Byron slot lengths before
     * the first non-Byron slot (or for every slot while that slot is not known yet).
     * The expression uses only numeric literals, so it is valid in PostgreSQL and DuckDB.
     */
    static String slotTimeSql(String slotColumn, EraService eraService) {
        long byronStart = eraService.blockTime(Era.Byron, 0);
        String byron = String.format("(%d + ROUND(%s * %s))", byronStart, slotColumn,
                slotLength(eraService, Era.Byron, 0));
        if (eraService.getFirstNonByronEpoch().isEmpty()) {
            return byron;
        }
        long firstShelleySlot = eraService.getFirstNonByronSlot();
        long shelleyStart = eraService.blockTime(Era.Shelley, firstShelleySlot);
        String shelley = String.format("(%d + ROUND((%s - %d) * %s))", shelleyStart, slotColumn, firstShelleySlot,
                slotLength(eraService, Era.Shelley, firstShelleySlot));
        return String.format("(CASE WHEN %s < %d THEN %s ELSE %s END)", slotColumn, firstShelleySlot, byron, shelley);
    }

    /** Slot length of an era in seconds, as an exact decimal literal (e.g. 20, 1, 0.2). */
    private static String slotLength(EraService eraService, Era era, long fromSlot) {
        long span = 1_000_000;
        long seconds = eraService.blockTime(era, fromSlot + span) - eraService.blockTime(era, fromSlot);
        return BigDecimal.valueOf(seconds).divide(BigDecimal.valueOf(span)).stripTrailingZeros().toPlainString();
    }
}

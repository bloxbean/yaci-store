package com.bloxbean.cardano.yaci.store.governanceaggr.service;

import com.bloxbean.cardano.yaci.store.dbutils.index.util.DatabaseUtils;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class DRepPv9ClearEventCacheService {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final String CACHE_MARKER_COUNT_QUERY = """
            SELECT COUNT(*)
            FROM drep_pv9_stale_clear_event_cache
            WHERE pv9_max_epoch = :pv9_max_epoch
            """;

    private static final String MAX_CACHED_SOURCE_SLOT_QUERY = """
            SELECT MAX(pv9_source_max_slot)
            FROM drep_pv9_stale_clear_event_cache
            """;

    private static final String DELETE_CACHE_MARKER_QUERY = """
            DELETE FROM drep_pv9_stale_clear_event_cache
            """;

    private static final String INSERT_CACHE_MARKER_QUERY = """
            INSERT INTO drep_pv9_stale_clear_event_cache
                (pv9_max_epoch, event_count, pv9_source_max_slot, update_datetime)
            VALUES
                (:pv9_max_epoch, :event_count, :pv9_source_max_slot, NOW())
            """;

    private static final String PV9_SOURCE_MAX_SLOT_QUERY = """
            SELECT COALESCE(MAX(source_slot), 0)
            FROM (
                SELECT slot AS source_slot
                FROM delegation_vote
                WHERE epoch <= :pv9_max_epoch
                UNION ALL
                SELECT slot AS source_slot
                FROM drep_registration
                WHERE epoch <= :pv9_max_epoch
                UNION ALL
                SELECT slot AS source_slot
                FROM stake_registration
                WHERE epoch <= :pv9_max_epoch
            ) pv9_source
            """;

    private static final String INSERT_CLEAR_EVENTS_QUERY = """
            INSERT INTO drep_pv9_stale_clear_event (
                pv9_max_epoch,
                address,
                old_drep_hash,
                old_drep_type,
                stale_slot,
                stale_tx_index,
                stale_cert_index,
                unreg_epoch,
                unreg_slot,
                unreg_tx_index,
                unreg_cert_index,
                update_datetime
            )
            SELECT DISTINCT
                :pv9_max_epoch,
                stale_del.address,
                stale_del.drep_hash,
                stale_del.drep_type,
                stale_del.slot,
                stale_del.tx_index,
                stale_del.cert_index,
                unreg.epoch,
                unreg.slot,
                unreg.tx_index,
                unreg.cert_index,
                NOW()
            FROM delegation_vote stale_del
            INNER JOIN drep_registration unreg
                -- Match the old DRep lifecycle that still contained the stale reverse entry.
                ON  unreg.drep_hash   = stale_del.drep_hash
                AND unreg.cred_type   = stale_del.drep_type
                AND unreg.type        = 'UNREG_DREP_CERT'
                AND unreg.epoch      <= :pv9_max_epoch
                AND (   unreg.slot > stale_del.slot
                     OR (unreg.slot = stale_del.slot AND unreg.tx_index > stale_del.tx_index)
                     OR (unreg.slot = stale_del.slot AND unreg.tx_index = stale_del.tx_index
                         AND unreg.cert_index > stale_del.cert_index))
            WHERE stale_del.address IS NOT NULL
            AND   stale_del.drep_type NOT IN ('ABSTAIN', 'NO_CONFIDENCE')
            AND   stale_del.epoch <= :pv9_max_epoch
            -- The stale delegation only matters if this DRep existed when that delegation was made.
            AND EXISTS (
                SELECT 1 FROM drep_registration reg_before_stale
                WHERE reg_before_stale.drep_hash = stale_del.drep_hash
                AND   reg_before_stale.cred_type = stale_del.drep_type
                AND   reg_before_stale.type      = 'REG_DREP_CERT'
                AND   reg_before_stale.epoch    <= :pv9_max_epoch
                AND (   reg_before_stale.slot < stale_del.slot
                     OR (reg_before_stale.slot = stale_del.slot
                         AND reg_before_stale.tx_index < stale_del.tx_index)
                     OR (reg_before_stale.slot = stale_del.slot
                         AND reg_before_stale.tx_index = stale_del.tx_index
                         AND reg_before_stale.cert_index < stale_del.cert_index))
                AND NOT EXISTS (
                    SELECT 1 FROM drep_registration unreg_before_stale
                    WHERE unreg_before_stale.drep_hash = stale_del.drep_hash
                    AND   unreg_before_stale.cred_type = stale_del.drep_type
                    AND   unreg_before_stale.type      = 'UNREG_DREP_CERT'
                    AND   unreg_before_stale.epoch    <= :pv9_max_epoch
                    AND (   unreg_before_stale.slot > reg_before_stale.slot
                         OR (unreg_before_stale.slot = reg_before_stale.slot
                             AND unreg_before_stale.tx_index > reg_before_stale.tx_index)
                         OR (unreg_before_stale.slot = reg_before_stale.slot
                             AND unreg_before_stale.tx_index = reg_before_stale.tx_index
                             AND unreg_before_stale.cert_index > reg_before_stale.cert_index))
                    AND (   unreg_before_stale.slot < stale_del.slot
                         OR (unreg_before_stale.slot = stale_del.slot
                             AND unreg_before_stale.tx_index < stale_del.tx_index)
                         OR (unreg_before_stale.slot = stale_del.slot
                             AND unreg_before_stale.tx_index = stale_del.tx_index
                             AND unreg_before_stale.cert_index < stale_del.cert_index))
                )
            )
            -- A previous unregistration between stale_del and this unreg belongs to an older DRep lifecycle.
            AND NOT EXISTS (
                SELECT 1 FROM drep_registration earlier_unreg
                WHERE earlier_unreg.drep_hash = stale_del.drep_hash
                AND   earlier_unreg.cred_type = stale_del.drep_type
                AND   earlier_unreg.type      = 'UNREG_DREP_CERT'
                AND   earlier_unreg.epoch    <= :pv9_max_epoch
                AND (   earlier_unreg.slot > stale_del.slot
                     OR (earlier_unreg.slot = stale_del.slot AND earlier_unreg.tx_index > stale_del.tx_index)
                     OR (earlier_unreg.slot = stale_del.slot AND earlier_unreg.tx_index = stale_del.tx_index
                         AND earlier_unreg.cert_index > stale_del.cert_index))
                AND (   earlier_unreg.slot < unreg.slot
                     OR (earlier_unreg.slot = unreg.slot AND earlier_unreg.tx_index < unreg.tx_index)
                     OR (earlier_unreg.slot = unreg.slot AND earlier_unreg.tx_index = unreg.tx_index
                         AND earlier_unreg.cert_index < unreg.cert_index))
            )
            -- If stake deregistration already removed the old DRep while it was current, this is not a stale clear.
            AND NOT EXISTS (
                SELECT 1 FROM stake_registration stake_dereg
                WHERE stake_dereg.address = stale_del.address
                AND   stake_dereg.type    = 'STAKE_DEREGISTRATION'
                AND   stake_dereg.epoch  <= :pv9_max_epoch
                AND (   stake_dereg.slot > stale_del.slot
                     OR (stake_dereg.slot = stale_del.slot AND stake_dereg.tx_index > stale_del.tx_index)
                     OR (stake_dereg.slot = stale_del.slot AND stake_dereg.tx_index = stale_del.tx_index
                         AND stake_dereg.cert_index > stale_del.cert_index))
                AND (   stake_dereg.slot < unreg.slot
                     OR (stake_dereg.slot = unreg.slot AND stake_dereg.tx_index < unreg.tx_index)
                     OR (stake_dereg.slot = unreg.slot AND stake_dereg.tx_index = unreg.tx_index
                         AND stake_dereg.cert_index < unreg.cert_index))
                AND EXISTS (
                    SELECT 1 FROM delegation_vote current_before_dereg
                    WHERE current_before_dereg.address   = stale_del.address
                    AND   current_before_dereg.drep_hash = stale_del.drep_hash
                    AND   current_before_dereg.drep_type = stale_del.drep_type
                    AND   current_before_dereg.epoch    <= :pv9_max_epoch
                    AND (   current_before_dereg.slot > stale_del.slot
                         OR (current_before_dereg.slot = stale_del.slot
                             AND current_before_dereg.tx_index > stale_del.tx_index)
                         OR (current_before_dereg.slot = stale_del.slot
                             AND current_before_dereg.tx_index = stale_del.tx_index
                             AND current_before_dereg.cert_index >= stale_del.cert_index))
                    AND (   current_before_dereg.slot < stake_dereg.slot
                         OR (current_before_dereg.slot = stake_dereg.slot
                             AND current_before_dereg.tx_index < stake_dereg.tx_index)
                         OR (current_before_dereg.slot = stake_dereg.slot
                             AND current_before_dereg.tx_index = stake_dereg.tx_index
                             AND current_before_dereg.cert_index < stake_dereg.cert_index))
                    AND NOT EXISTS (
                        SELECT 1 FROM delegation_vote later_del
                        WHERE later_del.address = stale_del.address
                        AND   later_del.epoch  <= :pv9_max_epoch
                        AND (   later_del.slot > current_before_dereg.slot
                             OR (later_del.slot = current_before_dereg.slot
                                 AND later_del.tx_index > current_before_dereg.tx_index)
                             OR (later_del.slot = current_before_dereg.slot
                                 AND later_del.tx_index = current_before_dereg.tx_index
                                 AND later_del.cert_index > current_before_dereg.cert_index))
                        AND (   later_del.slot < stake_dereg.slot
                             OR (later_del.slot = stake_dereg.slot
                                 AND later_del.tx_index < stake_dereg.tx_index)
                             OR (later_del.slot = stake_dereg.slot
                                 AND later_del.tx_index = stake_dereg.tx_index
                                 AND later_del.cert_index < stake_dereg.cert_index))
                    )
                )
            )
            -- Virtual delegations only suppress the stale clear if they actually displaced the old DRep.
            AND NOT EXISTS (
                SELECT 1 FROM delegation_vote virt_del
                WHERE virt_del.address   = stale_del.address
                AND   virt_del.drep_type IN ('ABSTAIN', 'NO_CONFIDENCE')
                AND   virt_del.epoch    <= :pv9_max_epoch
                AND (   virt_del.slot > stale_del.slot
                     OR (virt_del.slot = stale_del.slot AND virt_del.tx_index > stale_del.tx_index)
                     OR (virt_del.slot = stale_del.slot AND virt_del.tx_index = stale_del.tx_index
                         AND virt_del.cert_index > stale_del.cert_index))
                AND (   virt_del.slot < unreg.slot
                     OR (virt_del.slot = unreg.slot AND virt_del.tx_index < unreg.tx_index)
                     OR (virt_del.slot = unreg.slot AND virt_del.tx_index = unreg.tx_index
                         AND virt_del.cert_index < unreg.cert_index))
                AND EXISTS (
                    SELECT 1 FROM delegation_vote prev_del
                    WHERE prev_del.address   = stale_del.address
                    AND   prev_del.drep_hash = stale_del.drep_hash
                    AND   prev_del.drep_type = stale_del.drep_type
                    AND   prev_del.epoch    <= :pv9_max_epoch
                    AND (   prev_del.slot < virt_del.slot
                         OR (prev_del.slot = virt_del.slot AND prev_del.tx_index < virt_del.tx_index)
                         OR (prev_del.slot = virt_del.slot AND prev_del.tx_index = virt_del.tx_index
                             AND prev_del.cert_index < virt_del.cert_index))
                    AND NOT EXISTS (
                        SELECT 1 FROM delegation_vote between_del
                        WHERE between_del.address = stale_del.address
                        AND   between_del.epoch  <= :pv9_max_epoch
                        AND (   between_del.slot > prev_del.slot
                             OR (between_del.slot = prev_del.slot AND between_del.tx_index > prev_del.tx_index)
                             OR (between_del.slot = prev_del.slot AND between_del.tx_index = prev_del.tx_index
                                 AND between_del.cert_index > prev_del.cert_index))
                        AND (   between_del.slot < virt_del.slot
                             OR (between_del.slot = virt_del.slot AND between_del.tx_index < virt_del.tx_index)
                             OR (between_del.slot = virt_del.slot AND between_del.tx_index = virt_del.tx_index
                                 AND between_del.cert_index < virt_del.cert_index))
                    )
                )
                AND NOT EXISTS (
                    SELECT 1 FROM delegation_vote readd_del
                    WHERE readd_del.address   = stale_del.address
                    AND   readd_del.drep_hash = stale_del.drep_hash
                    AND   readd_del.drep_type = stale_del.drep_type
                    AND   readd_del.epoch    <= :pv9_max_epoch
                    AND (   readd_del.slot > virt_del.slot
                         OR (readd_del.slot = virt_del.slot AND readd_del.tx_index > virt_del.tx_index)
                         OR (readd_del.slot = virt_del.slot AND readd_del.tx_index = virt_del.tx_index
                             AND readd_del.cert_index > virt_del.cert_index))
                    AND (   readd_del.slot < unreg.slot
                         OR (readd_del.slot = unreg.slot AND readd_del.tx_index < unreg.tx_index)
                         OR (readd_del.slot = unreg.slot AND readd_del.tx_index = unreg.tx_index
                             AND readd_del.cert_index < unreg.cert_index))
                )
            )
            """;

    private static final String SNAPSHOT_CLEARED_ADDRESSES_QUERY = """
            CREATE %s TABLE ss_pv9_cleared_addresses AS
            SELECT DISTINCT e.address
            FROM drep_pv9_stale_clear_event e
            WHERE e.pv9_max_epoch = :pv9_max_epoch
            AND   e.unreg_epoch <= :epoch
            AND NOT EXISTS (
                SELECT 1
                FROM delegation_vote after_del
                WHERE after_del.address = e.address
                AND   after_del.epoch  <= :epoch
                AND (   after_del.slot > e.unreg_slot
                     OR (after_del.slot = e.unreg_slot AND after_del.tx_index > e.unreg_tx_index)
                     OR (after_del.slot = e.unreg_slot AND after_del.tx_index = e.unreg_tx_index
                         AND after_del.cert_index > e.unreg_cert_index))
            )
            """;

    @Transactional
    public synchronized void ensureCacheReady(int pv9MaxEpoch) {
        var params = new MapSqlParameterSource()
                .addValue("pv9_max_epoch", pv9MaxEpoch);

        Number markerCount = jdbcTemplate.queryForObject(CACHE_MARKER_COUNT_QUERY, params, Number.class);
        if (markerCount != null && markerCount.longValue() > 0) {
            return;
        }

        rebuildCache(pv9MaxEpoch);
    }

    @Transactional
    public synchronized void rebuildCache(int pv9MaxEpoch) {
        log.info("Rebuilding DRep PV9 stale clear-event cache for pv9MaxEpoch={}", pv9MaxEpoch);

        jdbcTemplate.update(DELETE_CACHE_MARKER_QUERY, Map.of());
        resetEventTable();

        var eventParams = new MapSqlParameterSource()
                .addValue("pv9_max_epoch", pv9MaxEpoch);
        int eventCount = jdbcTemplate.update(INSERT_CLEAR_EVENTS_QUERY, eventParams);
        long pv9SourceMaxSlot = getPv9SourceMaxSlot(eventParams);

        var markerParams = new MapSqlParameterSource()
                .addValue("pv9_max_epoch", pv9MaxEpoch)
                .addValue("event_count", eventCount)
                .addValue("pv9_source_max_slot", pv9SourceMaxSlot);
        jdbcTemplate.update(INSERT_CACHE_MARKER_QUERY, markerParams);

        log.info("DRep PV9 stale clear-event cache rebuilt with {} events and pv9SourceMaxSlot={}",
                eventCount, pv9SourceMaxSlot);
    }

    @Transactional
    public synchronized void invalidateCache() {
        jdbcTemplate.update(DELETE_CACHE_MARKER_QUERY, Map.of());
        resetEventTable();
        log.info("DRep PV9 stale clear-event cache invalidated");
    }

    public void createSnapshotClearedAddressesTable(String tableType, int epoch, int pv9MaxEpoch) {
        var params = new MapSqlParameterSource()
                .addValue("epoch", epoch)
                .addValue("pv9_max_epoch", pv9MaxEpoch);

        jdbcTemplate.update(String.format(SNAPSHOT_CLEARED_ADDRESSES_QUERY, tableType), params);
    }

    @EventListener
    @Transactional
    public void handleRollbackEvent(RollbackEvent rollbackEvent) {
        if (rollbackEvent.getRollbackTo() == null) {
            log.info("Invalidating DRep PV9 stale clear-event cache due to rollback event without rollback point");
            invalidateCache();
            return;
        }

        Number cachedSourceMaxSlot = jdbcTemplate.queryForObject(MAX_CACHED_SOURCE_SLOT_QUERY, Map.of(), Number.class);
        if (cachedSourceMaxSlot == null) {
            log.info("Skipping DRep PV9 stale clear-event cache invalidation; cache marker is absent");
            return;
        }

        long rollbackSlot = rollbackEvent.getRollbackTo().getSlot();
        long pv9SourceMaxSlot = cachedSourceMaxSlot.longValue();
        if (rollbackSlot <= pv9SourceMaxSlot) {
            log.info("Invalidating DRep PV9 stale clear-event cache because rollback slot {} <= pv9SourceMaxSlot {}",
                    rollbackSlot, pv9SourceMaxSlot);
            invalidateCache();
        } else {
            log.info("Keeping DRep PV9 stale clear-event cache because rollback slot {} > pv9SourceMaxSlot {}",
                    rollbackSlot, pv9SourceMaxSlot);
        }
    }

    private void resetEventTable() {
        var dbType = DatabaseUtils.getDbType(jdbcTemplate.getJdbcTemplate().getDataSource()).orElse(null);
        if (dbType == DatabaseUtils.DbType.mysql) {
            jdbcTemplate.update("DELETE FROM drep_pv9_stale_clear_event", Map.of());
        } else {
            jdbcTemplate.getJdbcTemplate().update("TRUNCATE TABLE drep_pv9_stale_clear_event");
        }
    }

    private long getPv9SourceMaxSlot(MapSqlParameterSource params) {
        Number sourceMaxSlot = jdbcTemplate.queryForObject(PV9_SOURCE_MAX_SLOT_QUERY, params, Number.class);
        return sourceMaxSlot == null ? 0 : sourceMaxSlot.longValue();
    }
}

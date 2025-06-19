package com.bloxbean.cardano.yaci.store.dbutils.index.service;

import com.bloxbean.cardano.yaci.store.dbutils.index.model.*;
import com.bloxbean.cardano.yaci.store.dbutils.index.util.DatabaseUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RollbackService {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final DatabaseUtils databaseUtils;

    @Transactional
    public Pair<List<TableRollbackAction>, Boolean> executeRollback(List<String> tableNames, RollbackContext context) {
        int epoch = context.getEpoch();
        long eventPublisherId = context.getEventPublisherId();

        RollbackBlock rollbackBlock = getRollbackBlockByEpoch(epoch);

        if (rollbackBlock == null) {
            log.error("Failed to get rollback block for epoch: {}", epoch);
            return Pair.of(new ArrayList<>(), false);
        }

        if (!context.isRollbackLedgerState()) {
            rollbackCursor(rollbackBlock, eventPublisherId);
            rollbackAccountConfig(rollbackBlock);
        }

        var params = new MapSqlParameterSource();
        params.addValue("epoch", rollbackBlock.getEpoch());
        params.addValue("slot", rollbackBlock.getSlot());

        List<TableRollbackAction> failedRollbackActions = new ArrayList<>();

        // Execute DELETE statements for each table/condition
        for (String tableName : tableNames) {
            if (databaseUtils.tableExists(tableName)) {
                String sql;
                if (context.isRollbackLedgerState() && tableName.equals("adapot_jobs")) {
                    sql = "UPDATE adapot_jobs SET status = 'NOT_STARTED' WHERE epoch >= :epoch";
                } else
                    sql = buildDeleteSql(tableName, rollbackBlock.getEpoch(), rollbackBlock.getSlot());
                log.info("Executing rollback on table '{}': {}", tableName, sql);
                try {
                    jdbcTemplate.update(sql, params);
                } catch (Exception e) {
                    log.error("Failed to execute rollback on table '{}': {}", tableName, e.getMessage());
                    failedRollbackActions.add(new TableRollbackAction(tableName, sql));
                }
            }
        }

        boolean rollbackSuccess = failedRollbackActions.isEmpty();

        return Pair.of(failedRollbackActions, rollbackSuccess);
    }

    private RollbackBlock getRollbackBlockByEpoch(int epoch) {
        // TODO: Handling for cases where there is no 'block' table
        String sql = "SELECT hash, slot, number, epoch_slot, era FROM block WHERE epoch = :epoch ORDER BY slot DESC LIMIT 1";

        var params = new MapSqlParameterSource()
                .addValue("epoch", epoch - 1);

        return jdbcTemplate.queryForObject(sql, params, (rs, rowNum) ->
                RollbackBlock.builder()
                        .hash(rs.getString("hash"))
                        .slot(rs.getLong("slot"))
                        .epoch(epoch)
                        .epochSlot(rs.getInt("epoch_slot"))
                        .number(rs.getLong("number"))
                        .era(Integer.valueOf(rs.getString("era")))
                        .build()

        );
    }

    private Integer getMaxEpoch() {
        String sql = "SELECT MAX(epoch) FROM block";
        return jdbcTemplate.getJdbcTemplate().queryForObject(sql, Integer.class);
    }

    public Pair<List<String>, List<String>> verifyRollbackActions(List<String> tableNames) {
        List<String> tableExists = new ArrayList<>();
        List<String> tableNotExists = new ArrayList<>();

        for (String tableName: tableNames) {
            if (databaseUtils.tableExists(tableName)) {
                tableExists.add(tableName);
            } else {
                tableNotExists.add(tableName);
            }
        }

        return Pair.of(tableExists, tableNotExists);
    }

    public boolean isValidRollbackEpoch(int epoch) {
        Integer maxEpoch = getMaxEpoch();

        if (maxEpoch == null) {
            log.error("Failed to get max epoch from block table");
            return false;
        }

        return epoch >= 1 && epoch <= maxEpoch;
    }

    private String buildDeleteSql(String table, int epoch, long slot) {
        String deleteFilter = buildDeleteFilter(table, epoch, slot);
        return "DELETE FROM " + table + " WHERE " + deleteFilter;
    }

    private String buildDeleteFilter(String tableName, int epoch, long slot) {
        if (tableName.equals("epoch_stake")) {
            return "epoch >= " + (epoch - 1);
        } else if (tableName.equals("drep_dist") || tableName.equals("gov_action_proposal_status")
                || tableName.equals("gov_epoch_activity") || tableName.equals("committee_state")) {
            return "epoch >= " + epoch;
        } else if (tableName.equals("tx_input")) {
            return "spent_at_slot > " + slot;
        }
        else {
            return "slot > " + slot;
        }
    }

    private void rollbackCursor(RollbackBlock rollbackBlock, long eventPublisherId) {
        String truncateCursor = "TRUNCATE TABLE cursor_";
        log.info("Truncating cursor_ table: {}", truncateCursor);
        jdbcTemplate.getJdbcTemplate().update(truncateCursor);

        String insertCursor =
                "INSERT INTO cursor_ (id, block_hash, slot, block_number, era) " +
                        "VALUES (:id, :block_hash, :slot, :block_number, :era)";
        log.info("Inserting into cursor_: {}", insertCursor);

        var params = new MapSqlParameterSource();
        params.addValue("id", eventPublisherId);
        params.addValue("slot", rollbackBlock.getSlot());
        params.addValue("block_number", rollbackBlock.getNumber());
        params.addValue("block_hash", rollbackBlock.getHash());
        params.addValue("era", rollbackBlock.getEra());

        jdbcTemplate.update(insertCursor, params);
    }

    private void rollbackAccountConfig(RollbackBlock rollbackBlock) {
        if (databaseUtils.tableExists("account_config")) {

            String truncateAccCfg = "TRUNCATE TABLE account_config";
            log.info("Truncating account_config: {}", truncateAccCfg);

            jdbcTemplate.getJdbcTemplate().update(truncateAccCfg);

            String insertAccCfg =
                    "INSERT INTO account_config (config_id, status, slot, block, block_hash) " +
                            "VALUES ('last_account_balance_processed_block', null, :slot, :block_number, :block_hash)";
            log.info("Inserting into account_config: {}", insertAccCfg);

            jdbcTemplate.update(insertAccCfg, new MapSqlParameterSource()
                    .addValue("slot", rollbackBlock.getSlot())
                    .addValue("block_number", rollbackBlock.getNumber())
                    .addValue("block_hash", rollbackBlock.getHash()));
        }
    }
}

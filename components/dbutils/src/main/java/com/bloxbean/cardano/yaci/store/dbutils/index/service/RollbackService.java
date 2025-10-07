package com.bloxbean.cardano.yaci.store.dbutils.index.service;

import com.bloxbean.cardano.yaci.store.dbutils.index.model.*;
import com.bloxbean.cardano.yaci.store.dbutils.index.util.DatabaseUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.common.time.SlotEpochService;
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
    private final SlotEpochService slotEpochService;

    @Transactional
    public Pair<List<TableRollbackAction>, Boolean> executeRollback(RollbackConfig config, RollbackContext context) {
        int epoch = context.getEpoch();
        long eventPublisherId = context.getEventPublisherId();

        RollbackBlock rollbackBlock = getRollbackBlockByEpoch(epoch);

        if (rollbackBlock == null) {
            log.error("Failed to get rollback block for epoch: {}", epoch);
            return Pair.of(new ArrayList<>(), false);
        }

        var params = new MapSqlParameterSource();
        params.addValue("epoch", rollbackBlock.getEpoch());
        params.addValue("slot", rollbackBlock.getSlot());

        List<TableRollbackAction> failedRollbackActions = new ArrayList<>();

        for (RollbackConfig.TableRollbackDefinition tableDef : config.getTables()) {
            String tableName = tableDef.getName();
            if (databaseUtils.tableExists(tableName)) {
                String sql;
                if ("UPDATE".equalsIgnoreCase(tableDef.getOperation())) {
                    sql = buildUpdateSql(tableDef);
                } else if ("DELETE".equalsIgnoreCase(tableDef.getOperation())) {
                    sql = buildDeleteSql(tableDef);
                } else {
                    throw new IllegalArgumentException("Invalid operation: " + tableDef.getOperation());
                }

                log.info("Executing rollback on table '{}': {}", tableName, sql);
                try {
                    jdbcTemplate.update(sql, params);
                } catch (Exception e) {
                    log.error("Failed to execute rollback on table '{}': {}", tableName, e.getMessage());
                    failedRollbackActions.add(new TableRollbackAction(tableName, sql));
                }
            }
        }

        if (!context.isRollbackLedgerState()) {
            rollbackCursor(rollbackBlock, eventPublisherId);
            rollbackAccountConfig(rollbackBlock);
        }

        boolean rollbackSuccess = failedRollbackActions.isEmpty();
        return Pair.of(failedRollbackActions, rollbackSuccess);
    }


    private RollbackBlock getRollbackBlockByEpoch(int epoch) {
        if (!databaseUtils.tableExists("cursor_")) {
            log.error("cursor_ table not found for rollback block");
            return null;
        }

        long shelleyStartSlot = slotEpochService.getFirstNonByronSlot();
        long byronSlotsPerEpoch = slotEpochService.slotsPerEpoch(Era.Byron);
        long shelleyStartEpoch = shelleyStartSlot / byronSlotsPerEpoch;

        Era targetEra = epoch < shelleyStartEpoch ? Era.Byron : Era.Shelley;

        long slotsPerEpoch = slotEpochService.slotsPerEpoch(targetEra);
        int lastEpochSlot = (int) (slotsPerEpoch - 1);

        long maxSlot;
        if (targetEra == Era.Byron) {
            maxSlot = (slotsPerEpoch * epoch) + lastEpochSlot;
        } else {
            maxSlot = slotEpochService.getShelleyAbsoluteSlot(epoch, lastEpochSlot);
        }

        String sql = "SELECT block_hash, slot, block_number, era FROM cursor_ WHERE slot >= :slot ORDER BY slot DESC LIMIT 1";
        log.info("Getting rollback block from cursor_ table: {}", sql);

        var params = new MapSqlParameterSource()
                .addValue("slot", maxSlot);

        return jdbcTemplate.queryForObject(sql, params, (rs, rowNum) ->
                RollbackBlock.builder()
                        .hash(rs.getString("block_hash"))
                        .slot(rs.getLong("slot"))
                        .epoch(epoch)
                        .number(rs.getLong("block_number"))
                        .era(rs.getInt("era"))
                        .build()
        );
    }

    private Integer getMaxEpoch() {
        if (!databaseUtils.tableExists("cursor_")) {
            log.error("cursor_ table not found for max epoch calculation");
            return null;
        }

        String sql = "SELECT MAX(slot) FROM cursor_";
        Integer epoch = null;
        Long maxSlot = jdbcTemplate.getJdbcTemplate().queryForObject(sql, Long.class);
        if (maxSlot != null) {
            long shelleyStartSlot = slotEpochService.getFirstNonByronSlot();
            if (maxSlot < shelleyStartSlot) {
                long byronSlotsPerEpoch = slotEpochService.slotsPerEpoch(Era.Byron);
                epoch = (int) (maxSlot / byronSlotsPerEpoch);
            } else {
                epoch = slotEpochService.getEpochNo(Era.Shelley, maxSlot);
            }
        }

        return epoch;
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
            log.error("Failed to get max epoch from cursor_ table");
            return false;
        }

        return epoch >= 1 && epoch <= maxEpoch;
    }

    private String buildDeleteSql(RollbackConfig.TableRollbackDefinition tableDef) {
        validateCondition(tableDef.getCondition());
        String deleteFilter = buildDeleteFilter(tableDef.getCondition());
        return "DELETE FROM " + tableDef.getName() + " WHERE " + deleteFilter;
    }

    private String buildUpdateSql(RollbackConfig.TableRollbackDefinition tableDef) {
        validateCondition(tableDef.getCondition());
        
        if (tableDef.getUpdateSet() == null || tableDef.getUpdateSet().isEmpty()) {
            throw new IllegalArgumentException("UPDATE operation requires 'update_set' clause for table: " + tableDef.getName());
        }
        
        StringBuilder updateSetClause = new StringBuilder();
        for (int i = 0; i < tableDef.getUpdateSet().size(); i++) {
            RollbackConfig.TableRollbackDefinition.UpdateSet update = tableDef.getUpdateSet().get(i);
            updateSetClause.append(update.getColumn()).append(" = ").append(update.getValue());
            if (i < tableDef.getUpdateSet().size() - 1) {
                updateSetClause.append(", ");
            }
        }
        String conditionFilter = buildUpdateConditionFilter(tableDef.getCondition());
        return String.format("UPDATE %s SET %s WHERE %s", tableDef.getName(), updateSetClause.toString(), conditionFilter);
    }

    private String buildDeleteFilter(RollbackConfig.TableRollbackDefinition.Condition condition) {
        String column = condition.getColumn();
        String operator = condition.getOperator();
        String type = condition.getType();
        Integer offset = condition.getOffset();

        if ("epoch".equalsIgnoreCase(type)) {
            if (offset != null && offset != 0) {
                return String.format("%s %s (:epoch + %d)", column, operator, offset);
            } else {
                return String.format("%s %s :epoch", column, operator);
            }
        } else if ("slot".equalsIgnoreCase(type)) {
            if (offset != null && offset != 0) {
                return String.format("%s %s (:slot + %d)", column, operator, offset);
            } else {
                return String.format("%s %s :slot", column, operator);
            }
        } else {
            return "1=1"; // Should not happen with proper config
        }
    }

    private String buildUpdateConditionFilter(RollbackConfig.TableRollbackDefinition.Condition condition) {
        String column = condition.getColumn();
        String operator = condition.getOperator();
        String type = condition.getType();
        Integer offset = condition.getOffset();

        if ("epoch".equalsIgnoreCase(type)) {
            if (offset != null) {
                return String.format("%s %s (:epoch + %d)", column, operator, offset);
            } else {
                return String.format("%s %s :epoch", column, operator);
            }
        } else if ("slot".equalsIgnoreCase(type)) {
            if (offset != null) {
                return String.format("%s %s (:slot + %d)", column, operator, offset);
            } else {
                return String.format("%s %s :slot", column, operator);
            }
        } else {
            return "1=1"; // Should not happen for update conditions
        }
    }

    private void validateCondition(RollbackConfig.TableRollbackDefinition.Condition condition) {
        String column = condition.getColumn();
        String operator = condition.getOperator();
        
        if (column == null || column.trim().isEmpty()) {
            throw new IllegalArgumentException("Column name cannot be null or empty");
        }
        
        if (operator == null || operator.trim().isEmpty()) {
            throw new IllegalArgumentException("Operator cannot be null or empty");
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

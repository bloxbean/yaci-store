package com.bloxbean.cardano.yaci.store.dbutils.index.service;

import com.bloxbean.cardano.yaci.store.common.service.IEraService;
import com.bloxbean.cardano.yaci.store.dbutils.index.model.RollbackBlock;
import com.bloxbean.cardano.yaci.store.dbutils.index.model.RollbackContext;
import com.bloxbean.cardano.yaci.store.dbutils.index.model.RollbackConfig;
import com.bloxbean.cardano.yaci.store.dbutils.index.model.TableRollbackAction;
import com.bloxbean.cardano.yaci.store.dbutils.index.util.DatabaseUtils;
import com.bloxbean.cardano.yaci.store.dbutils.index.util.RollbackLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.util.Pair;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RollbackServiceTest {

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Mock
    private JdbcTemplate plainJdbcTemplate;

    @Mock
    private DatabaseUtils databaseUtils;

    @Mock
    private IEraService eraService;

    private RollbackLoader configLoader;
    private RollbackService rollbackService;

    @BeforeEach
    void setUp() {
        configLoader = new RollbackLoader();
        rollbackService = new RollbackService(jdbcTemplate, databaseUtils, eraService);
    }

    @Test
    void testExecuteRollback_WithRealYamlConfig_ShouldGenerateExpectedSQL() {
        String configFilePath = "rollback.yml";
        RollbackContext context = RollbackContext.builder()
                .epoch(100)
                .eventPublisherId(1L)
                .rollbackLedgerState(false)
                .build();

        when(databaseUtils.tableExists(anyString())).thenReturn(true);
        when(jdbcTemplate.queryForObject(anyString(), any(SqlParameterSource.class), any(RowMapper.class))).thenReturn(
                createMockRollbackBlock(100, 1000L, "block_hash_123", 2000L, 50, 5)
        );
        when(jdbcTemplate.update(anyString(), any(SqlParameterSource.class))).thenReturn(1);
        when(jdbcTemplate.getJdbcTemplate()).thenReturn(plainJdbcTemplate);
        when(plainJdbcTemplate.update(anyString())).thenReturn(1);

        RollbackConfig config = configLoader.loadRollbackConfig(configFilePath);
        Pair<List<TableRollbackAction>, Boolean> result = rollbackService.executeRollback(config, context);

        assertNotNull(result);
        assertTrue(result.getSecond());

        verify(jdbcTemplate, times(1)).update(eq("DELETE FROM asset WHERE slot > :slot"), any(SqlParameterSource.class));
        verify(jdbcTemplate, times(1)).update(eq("DELETE FROM block WHERE slot > :slot"), any(SqlParameterSource.class));
        verify(jdbcTemplate, times(1)).update(eq("DELETE FROM epoch_stake WHERE epoch >= (:epoch + -1)"), any(SqlParameterSource.class));
        verify(jdbcTemplate, times(1)).update(eq("DELETE FROM tx_input WHERE spent_at_slot > :slot"), any(SqlParameterSource.class));
        verify(jdbcTemplate, times(1)).update(eq("DELETE FROM adapot_jobs WHERE slot > :slot"), any(SqlParameterSource.class));
        verify(jdbcTemplate, times(1)).update(eq("DELETE FROM drep_dist WHERE epoch >= :epoch"), any(SqlParameterSource.class));
        verify(jdbcTemplate, times(1)).update(eq("DELETE FROM gov_action_proposal_status WHERE epoch >= :epoch"), any(SqlParameterSource.class));
        verify(jdbcTemplate, times(1)).update(eq("DELETE FROM gov_epoch_activity WHERE epoch >= :epoch"), any(SqlParameterSource.class));
        verify(jdbcTemplate, times(1)).update(eq("DELETE FROM committee_state WHERE epoch >= :epoch"), any(SqlParameterSource.class));

        verify(plainJdbcTemplate, times(1)).update(contains("TRUNCATE TABLE cursor_"));
        verify(jdbcTemplate, times(1)).update(contains("INSERT INTO cursor_"), any(SqlParameterSource.class));
        verify(plainJdbcTemplate, times(1)).update(contains("TRUNCATE TABLE account_config"));
        verify(jdbcTemplate, times(1)).update(contains("INSERT INTO account_config"), any(SqlParameterSource.class));
    }

    @Test
    void testExecuteRollback_WithDifferentEpochs_ShouldGenerateCorrectSQL() {
        String configFilePath = "rollback.yml";
        RollbackContext context = RollbackContext.builder()
                .epoch(50)
                .eventPublisherId(1L)
                .rollbackLedgerState(false)
                .build();

        when(databaseUtils.tableExists(anyString())).thenReturn(true);
        when(jdbcTemplate.queryForObject(anyString(), any(SqlParameterSource.class), any(RowMapper.class))).thenReturn(
                createMockRollbackBlock(50, 500L, "block_hash_456", 1000L, 25, 5)
        );
        when(jdbcTemplate.update(anyString(), any(SqlParameterSource.class))).thenReturn(1);
        when(jdbcTemplate.getJdbcTemplate()).thenReturn(plainJdbcTemplate);
        when(plainJdbcTemplate.update(anyString())).thenReturn(1);

        RollbackConfig config = configLoader.loadRollbackConfig(configFilePath);
        Pair<List<TableRollbackAction>, Boolean> result = rollbackService.executeRollback(config, context);

        assertNotNull(result);
        assertTrue(result.getSecond());

        verify(jdbcTemplate, times(1)).update(eq("DELETE FROM asset WHERE slot > :slot"), any(SqlParameterSource.class));
        verify(jdbcTemplate, times(1)).update(eq("DELETE FROM epoch_stake WHERE epoch >= (:epoch + -1)"), any(SqlParameterSource.class));
        verify(jdbcTemplate, times(1)).update(eq("DELETE FROM tx_input WHERE spent_at_slot > :slot"), any(SqlParameterSource.class));
        verify(jdbcTemplate, times(1)).update(eq("DELETE FROM adapot_jobs WHERE slot > :slot"), any(SqlParameterSource.class));
    }

    @Test
    void testIsValidRollbackEpoch_WithValidEpoch_ShouldReturnTrue() {
        when(databaseUtils.tableExists("block")).thenReturn(true);
        when(jdbcTemplate.getJdbcTemplate()).thenReturn(plainJdbcTemplate);
        when(plainJdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(200);

        boolean result = rollbackService.isValidRollbackEpoch(100);

        assertTrue(result);
    }

    @Test
    void testIsValidRollbackEpoch_WithInvalidEpoch_ShouldReturnFalse() {
        when(databaseUtils.tableExists("block")).thenReturn(true);
        when(jdbcTemplate.getJdbcTemplate()).thenReturn(plainJdbcTemplate);
        when(plainJdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(50);

        boolean result = rollbackService.isValidRollbackEpoch(100);

        assertFalse(result);
    }

    @Test
    void testExecuteRollback_WithManualRollbackPoint_ShouldSucceed() {
        String configFilePath = "rollback.yml";
        RollbackContext context = RollbackContext.builder()
                .epoch(100)
                .eventPublisherId(1L)
                .rollbackLedgerState(false)
                .rollbackPointBlock(2000L)
                .rollbackPointBlockHash("manual_block_hash_123")
                .rollbackPointSlot(1000L)
                .build();

        // Mock tableExists to return false for "block" table, true for others
        when(databaseUtils.tableExists(anyString())).thenAnswer(invocation -> {
            String tableName = invocation.getArgument(0);
            return !"block".equals(tableName);
        });
        when(eraService.getEraForEpoch(100)).thenReturn(com.bloxbean.cardano.yaci.core.model.Era.Shelley);
        when(jdbcTemplate.update(anyString(), any(SqlParameterSource.class))).thenReturn(1);
        when(jdbcTemplate.getJdbcTemplate()).thenReturn(plainJdbcTemplate);
        when(plainJdbcTemplate.update(anyString())).thenReturn(1);

        RollbackConfig config = configLoader.loadRollbackConfig(configFilePath);
        Pair<List<TableRollbackAction>, Boolean> result = rollbackService.executeRollback(config, context);

        assertNotNull(result);
        assertTrue(result.getSecond());

        verify(eraService, times(1)).getEraForEpoch(100);
        verify(jdbcTemplate, times(1)).update(eq("DELETE FROM asset WHERE slot > :slot"), any(SqlParameterSource.class));
    }

    @Test
    void testExecuteRollback_WithoutBlockTable_WithoutManualPoint_ShouldThrowException() {
        RollbackContext context = RollbackContext.builder()
                .epoch(100)
                .eventPublisherId(1L)
                .rollbackLedgerState(false)
                .build();

        when(databaseUtils.tableExists("block")).thenReturn(false);

        RollbackConfig config = configLoader.loadRollbackConfig("rollback.yml");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            rollbackService.executeRollback(config, context);
        });

        assertTrue(exception.getMessage().contains("Block table not available"));
        assertTrue(exception.getMessage().contains("manual rollback point not provided"));
    }

    @Test
    void testIsValidRollbackEpoch_WithoutBlockTable_WithCursorTable_ShouldReturnTrue() {
        when(databaseUtils.tableExists("block")).thenReturn(false);
        when(databaseUtils.tableExists("cursor_")).thenReturn(true);
        when(jdbcTemplate.getJdbcTemplate()).thenReturn(plainJdbcTemplate);
        when(plainJdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(50000000L);
        when(eraService.getFirstNonByronSlot()).thenReturn(4492800L);
        when(eraService.getEpochNo(any(), anyLong())).thenReturn(200);

        boolean result = rollbackService.isValidRollbackEpoch(100);

        assertTrue(result);
    }

    @Test
    void testIsValidRollbackEpoch_WithoutBlockTable_ByronEra_ShouldCalculateCorrectly() {
        when(databaseUtils.tableExists("block")).thenReturn(false);
        when(databaseUtils.tableExists("cursor_")).thenReturn(true);
        when(jdbcTemplate.getJdbcTemplate()).thenReturn(plainJdbcTemplate);
        when(plainJdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(2000000L);
        when(eraService.getFirstNonByronSlot()).thenReturn(4492800L);
        when(eraService.slotsPerEpoch(com.bloxbean.cardano.yaci.core.model.Era.Byron)).thenReturn(21600L);

        boolean result = rollbackService.isValidRollbackEpoch(50);

        assertTrue(result);
    }

    @Test
    void testExecuteRollback_WithRollbackLedgerState_ShouldSkipCursorAndAccountConfig() {
        String configFilePath = "rollback.yml";
        RollbackContext context = RollbackContext.builder()
                .epoch(100)
                .eventPublisherId(1L)
                .rollbackLedgerState(true) // Skip cursor and account_config rollback
                .build();

        when(databaseUtils.tableExists(anyString())).thenReturn(true);
        when(jdbcTemplate.queryForObject(anyString(), any(SqlParameterSource.class), any(RowMapper.class))).thenReturn(
                createMockRollbackBlock(100, 1000L, "block_hash_123", 2000L, 50, 5)
        );
        when(jdbcTemplate.update(anyString(), any(SqlParameterSource.class))).thenReturn(1);

        RollbackConfig config = configLoader.loadRollbackConfig(configFilePath);
        Pair<List<TableRollbackAction>, Boolean> result = rollbackService.executeRollback(config, context);

        assertNotNull(result);
        assertTrue(result.getSecond());

        // Verify cursor_ and account_config were NOT modified
        verify(jdbcTemplate, never()).getJdbcTemplate();
        verify(jdbcTemplate, times(1)).update(eq("DELETE FROM asset WHERE slot > :slot"), any(SqlParameterSource.class));
    }

    @Test
    void testExecuteRollback_WithFailedTableRollback_ShouldReturnFailedActions() {
        String configFilePath = "rollback.yml";
        RollbackContext context = RollbackContext.builder()
                .epoch(100)
                .eventPublisherId(1L)
                .rollbackLedgerState(false)
                .build();

        when(databaseUtils.tableExists(anyString())).thenReturn(true);
        when(jdbcTemplate.queryForObject(anyString(), any(SqlParameterSource.class), any(RowMapper.class))).thenReturn(
                createMockRollbackBlock(100, 1000L, "block_hash_123", 2000L, 50, 5)
        );
        when(jdbcTemplate.getJdbcTemplate()).thenReturn(plainJdbcTemplate);
        when(plainJdbcTemplate.update(anyString())).thenReturn(1);

        // Simulate failure for specific table
        when(jdbcTemplate.update(contains("DELETE FROM asset"), any(SqlParameterSource.class)))
                .thenThrow(new RuntimeException("Database error"));
        when(jdbcTemplate.update(argThat(sql -> sql != null && !sql.contains("DELETE FROM asset")), any(SqlParameterSource.class))).thenReturn(1);

        RollbackConfig config = configLoader.loadRollbackConfig(configFilePath);
        Pair<List<TableRollbackAction>, Boolean> result = rollbackService.executeRollback(config, context);

        assertNotNull(result);
        assertFalse(result.getSecond()); // Should be false due to failure
        assertFalse(result.getFirst().isEmpty()); // Should have failed actions
        assertEquals(1, result.getFirst().size());
        assertEquals("asset", result.getFirst().get(0).getTableName());
    }

    @Test
    void testExecuteRollback_WithNonExistentTable_ShouldSkipIt() {
        String configFilePath = "rollback.yml";
        RollbackContext context = RollbackContext.builder()
                .epoch(100)
                .eventPublisherId(1L)
                .rollbackLedgerState(false)
                .build();

        // Only "block" and "asset" tables exist
        when(databaseUtils.tableExists(anyString())).thenAnswer(invocation -> {
            String tableName = invocation.getArgument(0);
            return "block".equals(tableName) || "asset".equals(tableName) || "cursor_".equals(tableName) || "account_config".equals(tableName);
        });
        when(jdbcTemplate.queryForObject(anyString(), any(SqlParameterSource.class), any(RowMapper.class))).thenReturn(
                createMockRollbackBlock(100, 1000L, "block_hash_123", 2000L, 50, 5)
        );
        when(jdbcTemplate.update(anyString(), any(SqlParameterSource.class))).thenReturn(1);
        when(jdbcTemplate.getJdbcTemplate()).thenReturn(plainJdbcTemplate);
        when(plainJdbcTemplate.update(anyString())).thenReturn(1);

        RollbackConfig config = configLoader.loadRollbackConfig(configFilePath);
        Pair<List<TableRollbackAction>, Boolean> result = rollbackService.executeRollback(config, context);

        assertNotNull(result);
        assertTrue(result.getSecond());

        // Should only execute for existing tables
        verify(jdbcTemplate, times(1)).update(eq("DELETE FROM asset WHERE slot > :slot"), any(SqlParameterSource.class));
        verify(jdbcTemplate, times(1)).update(eq("DELETE FROM block WHERE slot > :slot"), any(SqlParameterSource.class));
        // Other tables should be skipped
        verify(jdbcTemplate, never()).update(eq("DELETE FROM epoch_stake WHERE epoch >= (:epoch + -1)"), any(SqlParameterSource.class));
    }

    @Test
    void testVerifyRollbackActions_WithMixedTables_ShouldReturnCorrectLists() {
        when(databaseUtils.tableExists("asset")).thenReturn(true);
        when(databaseUtils.tableExists("block")).thenReturn(true);
        when(databaseUtils.tableExists("non_existent_table")).thenReturn(false);
        when(databaseUtils.tableExists("another_missing_table")).thenReturn(false);

        List<String> tableNames = List.of("asset", "block", "non_existent_table", "another_missing_table");
        Pair<List<String>, List<String>> result = rollbackService.verifyRollbackActions(tableNames);

        assertNotNull(result);
        assertEquals(2, result.getFirst().size());
        assertTrue(result.getFirst().contains("asset"));
        assertTrue(result.getFirst().contains("block"));

        assertEquals(2, result.getSecond().size());
        assertTrue(result.getSecond().contains("non_existent_table"));
        assertTrue(result.getSecond().contains("another_missing_table"));
    }

    @Test
    void testIsValidRollbackEpoch_WithEpochZero_ShouldReturnFalse() {
        when(databaseUtils.tableExists("block")).thenReturn(true);
        when(jdbcTemplate.getJdbcTemplate()).thenReturn(plainJdbcTemplate);
        when(plainJdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(200);

        boolean result = rollbackService.isValidRollbackEpoch(0);

        assertFalse(result);
    }

    @Test
    void testIsValidRollbackEpoch_WithNegativeEpoch_ShouldReturnFalse() {
        when(databaseUtils.tableExists("block")).thenReturn(true);
        when(jdbcTemplate.getJdbcTemplate()).thenReturn(plainJdbcTemplate);
        when(plainJdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(200);

        boolean result = rollbackService.isValidRollbackEpoch(-5);

        assertFalse(result);
    }

    @Test
    void testIsValidRollbackEpoch_WithNullMaxEpoch_ShouldReturnFalse() {
        when(databaseUtils.tableExists("block")).thenReturn(false);
        when(databaseUtils.tableExists("cursor_")).thenReturn(false);

        boolean result = rollbackService.isValidRollbackEpoch(100);

        assertFalse(result);
    }

    @Test
    void testIsValidRollbackEpoch_WithNullSlotFromCursor_ShouldReturnFalse() {
        when(databaseUtils.tableExists("block")).thenReturn(false);
        when(databaseUtils.tableExists("cursor_")).thenReturn(true);
        when(jdbcTemplate.getJdbcTemplate()).thenReturn(plainJdbcTemplate);
        when(plainJdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(null);

        boolean result = rollbackService.isValidRollbackEpoch(100);

        assertFalse(result);
    }

    @Test
    void testExecuteRollback_WithoutAccountConfigTable_ShouldSkipAccountConfigRollback() {
        String configFilePath = "rollback.yml";
        RollbackContext context = RollbackContext.builder()
                .epoch(100)
                .eventPublisherId(1L)
                .rollbackLedgerState(false)
                .build();

        when(databaseUtils.tableExists(anyString())).thenAnswer(invocation -> {
            String tableName = invocation.getArgument(0);
            return !"account_config".equals(tableName); // account_config doesn't exist
        });
        when(jdbcTemplate.queryForObject(anyString(), any(SqlParameterSource.class), any(RowMapper.class))).thenReturn(
                createMockRollbackBlock(100, 1000L, "block_hash_123", 2000L, 50, 5)
        );
        when(jdbcTemplate.update(anyString(), any(SqlParameterSource.class))).thenReturn(1);
        when(jdbcTemplate.getJdbcTemplate()).thenReturn(plainJdbcTemplate);
        when(plainJdbcTemplate.update(anyString())).thenReturn(1);

        RollbackConfig config = configLoader.loadRollbackConfig(configFilePath);
        Pair<List<TableRollbackAction>, Boolean> result = rollbackService.executeRollback(config, context);

        assertNotNull(result);
        assertTrue(result.getSecond());

        // Verify cursor_ was updated but account_config was not
        verify(plainJdbcTemplate, times(1)).update(contains("TRUNCATE TABLE cursor_"));
        verify(jdbcTemplate, times(1)).update(contains("INSERT INTO cursor_"), any(SqlParameterSource.class));
        verify(plainJdbcTemplate, never()).update(contains("TRUNCATE TABLE account_config"));
        verify(jdbcTemplate, never()).update(contains("INSERT INTO account_config"), any(SqlParameterSource.class));
    }

    @Test
    void testExecuteRollback_WithMissingSlotInManualRollback_ShouldThrowException() {
        RollbackContext context = RollbackContext.builder()
                .epoch(100)
                .eventPublisherId(1L)
                .rollbackLedgerState(false)
                .rollbackPointBlock(2000L)
                .rollbackPointBlockHash("hash123")
                .rollbackPointSlot(null) // Missing slot
                .build();

        when(databaseUtils.tableExists("block")).thenReturn(false);

        RollbackConfig config = configLoader.loadRollbackConfig("rollback.yml");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            rollbackService.executeRollback(config, context);
        });

        assertTrue(exception.getMessage().contains("Block table not available"));
        assertTrue(exception.getMessage().contains("manual rollback point not provided"));
    }

    @Test
    void testExecuteRollback_WithMissingBlockHashInManualRollback_ShouldThrowException() {
        RollbackContext context = RollbackContext.builder()
                .epoch(100)
                .eventPublisherId(1L)
                .rollbackLedgerState(false)
                .rollbackPointBlock(2000L)
                .rollbackPointBlockHash(null) // Missing block hash
                .rollbackPointSlot(1000L)
                .build();

        when(databaseUtils.tableExists("block")).thenReturn(false);

        RollbackConfig config = configLoader.loadRollbackConfig("rollback.yml");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            rollbackService.executeRollback(config, context);
        });

        assertTrue(exception.getMessage().contains("Block table not available"));
        assertTrue(exception.getMessage().contains("manual rollback point not provided"));
    }

    @Test
    void testExecuteRollback_WithInvalidOperation_ShouldThrowException() {
        RollbackConfig config = RollbackConfig.builder()
                .tables(List.of(
                        RollbackConfig.TableRollbackDefinition.builder()
                                .name("test_table")
                                .operation("INVALID_OPERATION") // Invalid operation
                                .condition(RollbackConfig.TableRollbackDefinition.Condition.builder()
                                        .type("slot")
                                        .column("slot")
                                        .operator(">")
                                        .build())
                                .build()
                ))
                .build();

        RollbackContext context = RollbackContext.builder()
                .epoch(100)
                .eventPublisherId(1L)
                .rollbackLedgerState(false)
                .build();

        when(databaseUtils.tableExists(anyString())).thenReturn(true);
        when(jdbcTemplate.queryForObject(anyString(), any(SqlParameterSource.class), any(RowMapper.class))).thenReturn(
                createMockRollbackBlock(100, 1000L, "block_hash_123", 2000L, 50, 5)
        );
        when(jdbcTemplate.getJdbcTemplate()).thenReturn(plainJdbcTemplate);
        when(plainJdbcTemplate.update(anyString())).thenReturn(1);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            rollbackService.executeRollback(config, context);
        });

        assertTrue(exception.getMessage().contains("Invalid operation"));
    }

    @Test
    void testExecuteRollback_WithUpdateOperationMissingUpdateSet_ShouldThrowException() {
        RollbackConfig config = RollbackConfig.builder()
                .tables(List.of(
                        RollbackConfig.TableRollbackDefinition.builder()
                                .name("test_table")
                                .operation("UPDATE")
                                .condition(RollbackConfig.TableRollbackDefinition.Condition.builder()
                                        .type("epoch")
                                        .column("epoch")
                                        .operator(">=")
                                        .build())
                                // Missing updateSet
                                .build()
                ))
                .build();

        RollbackContext context = RollbackContext.builder()
                .epoch(100)
                .eventPublisherId(1L)
                .rollbackLedgerState(false)
                .build();

        when(databaseUtils.tableExists(anyString())).thenReturn(true);
        when(jdbcTemplate.queryForObject(anyString(), any(SqlParameterSource.class), any(RowMapper.class))).thenReturn(
                createMockRollbackBlock(100, 1000L, "block_hash_123", 2000L, 50, 5)
        );
        when(jdbcTemplate.getJdbcTemplate()).thenReturn(plainJdbcTemplate);
        when(plainJdbcTemplate.update(anyString())).thenReturn(1);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            rollbackService.executeRollback(config, context);
        });

        assertTrue(exception.getMessage().contains("UPDATE operation requires 'update_set' clause"));
    }

    @Test
    void testBuildUpdateSql_WithMultipleUpdateColumns_ShouldGenerateCorrectSQL() {
        RollbackConfig config = RollbackConfig.builder()
                .tables(List.of(
                        RollbackConfig.TableRollbackDefinition.builder()
                                .name("test_table")
                                .operation("UPDATE")
                                .condition(RollbackConfig.TableRollbackDefinition.Condition.builder()
                                        .type("epoch")
                                        .column("epoch")
                                        .operator(">=")
                                        .build())
                                .updateSet(List.of(
                                        RollbackConfig.TableRollbackDefinition.UpdateSet.builder()
                                                .column("status")
                                                .value("'PENDING'")
                                                .build(),
                                        RollbackConfig.TableRollbackDefinition.UpdateSet.builder()
                                                .column("updated_at")
                                                .value("NULL")
                                                .build()
                                ))
                                .build()
                ))
                .build();

        RollbackContext context = RollbackContext.builder()
                .epoch(100)
                .eventPublisherId(1L)
                .rollbackLedgerState(false)
                .build();

        when(databaseUtils.tableExists(anyString())).thenReturn(true);
        when(jdbcTemplate.queryForObject(anyString(), any(SqlParameterSource.class), any(RowMapper.class))).thenReturn(
                createMockRollbackBlock(100, 1000L, "block_hash_123", 2000L, 50, 5)
        );
        when(jdbcTemplate.update(anyString(), any(SqlParameterSource.class))).thenReturn(1);
        when(jdbcTemplate.getJdbcTemplate()).thenReturn(plainJdbcTemplate);
        when(plainJdbcTemplate.update(anyString())).thenReturn(1);

        Pair<List<TableRollbackAction>, Boolean> result = rollbackService.executeRollback(config, context);

        assertNotNull(result);
        assertTrue(result.getSecond());

        verify(jdbcTemplate, times(1)).update(
                eq("UPDATE test_table SET status = 'PENDING', updated_at = NULL WHERE epoch >= :epoch"),
                any(SqlParameterSource.class)
        );
    }

    @Test
    void testBuildDeleteSql_WithOffsetZero_ShouldNotIncludeOffset() {
        RollbackConfig config = RollbackConfig.builder()
                .tables(List.of(
                        RollbackConfig.TableRollbackDefinition.builder()
                                .name("test_table")
                                .operation("DELETE")
                                .condition(RollbackConfig.TableRollbackDefinition.Condition.builder()
                                        .type("slot")
                                        .column("slot")
                                        .operator(">")
                                        .offset(0) // Offset is 0
                                        .build())
                                .build()
                ))
                .build();

        RollbackContext context = RollbackContext.builder()
                .epoch(100)
                .eventPublisherId(1L)
                .rollbackLedgerState(false)
                .build();

        when(databaseUtils.tableExists(anyString())).thenReturn(true);
        when(jdbcTemplate.queryForObject(anyString(), any(SqlParameterSource.class), any(RowMapper.class))).thenReturn(
                createMockRollbackBlock(100, 1000L, "block_hash_123", 2000L, 50, 5)
        );
        when(jdbcTemplate.update(anyString(), any(SqlParameterSource.class))).thenReturn(1);
        when(jdbcTemplate.getJdbcTemplate()).thenReturn(plainJdbcTemplate);
        when(plainJdbcTemplate.update(anyString())).thenReturn(1);

        Pair<List<TableRollbackAction>, Boolean> result = rollbackService.executeRollback(config, context);

        assertNotNull(result);
        assertTrue(result.getSecond());

        verify(jdbcTemplate, times(1)).update(
                eq("DELETE FROM test_table WHERE slot > :slot"),
                any(SqlParameterSource.class)
        );
    }

    @Test
    void testValidateCondition_WithNullColumn_ShouldThrowException() {
        RollbackConfig config = RollbackConfig.builder()
                .tables(List.of(
                        RollbackConfig.TableRollbackDefinition.builder()
                                .name("test_table")
                                .operation("DELETE")
                                .condition(RollbackConfig.TableRollbackDefinition.Condition.builder()
                                        .type("slot")
                                        .column(null) // Null column
                                        .operator(">")
                                        .build())
                                .build()
                ))
                .build();

        RollbackContext context = RollbackContext.builder()
                .epoch(100)
                .eventPublisherId(1L)
                .rollbackLedgerState(false)
                .build();

        when(databaseUtils.tableExists(anyString())).thenReturn(true);
        when(jdbcTemplate.queryForObject(anyString(), any(SqlParameterSource.class), any(RowMapper.class))).thenReturn(
                createMockRollbackBlock(100, 1000L, "block_hash_123", 2000L, 50, 5)
        );
        when(jdbcTemplate.getJdbcTemplate()).thenReturn(plainJdbcTemplate);
        when(plainJdbcTemplate.update(anyString())).thenReturn(1);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            rollbackService.executeRollback(config, context);
        });

        assertTrue(exception.getMessage().contains("Column name cannot be null or empty"));
    }

    @Test
    void testValidateCondition_WithNullOperator_ShouldThrowException() {
        RollbackConfig config = RollbackConfig.builder()
                .tables(List.of(
                        RollbackConfig.TableRollbackDefinition.builder()
                                .name("test_table")
                                .operation("DELETE")
                                .condition(RollbackConfig.TableRollbackDefinition.Condition.builder()
                                        .type("slot")
                                        .column("slot")
                                        .operator(null) // Null operator
                                        .build())
                                .build()
                ))
                .build();

        RollbackContext context = RollbackContext.builder()
                .epoch(100)
                .eventPublisherId(1L)
                .rollbackLedgerState(false)
                .build();

        when(databaseUtils.tableExists(anyString())).thenReturn(true);
        when(jdbcTemplate.queryForObject(anyString(), any(SqlParameterSource.class), any(RowMapper.class))).thenReturn(
                createMockRollbackBlock(100, 1000L, "block_hash_123", 2000L, 50, 5)
        );
        when(jdbcTemplate.getJdbcTemplate()).thenReturn(plainJdbcTemplate);
        when(plainJdbcTemplate.update(anyString())).thenReturn(1);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            rollbackService.executeRollback(config, context);
        });

        assertTrue(exception.getMessage().contains("Operator cannot be null or empty"));
    }

    private RollbackBlock createMockRollbackBlock(int epoch, long slot, String hash, long number, int epochSlot, int era) {
        return RollbackBlock.builder()
                .epoch(epoch)
                .slot(slot)
                .hash(hash)
                .number(number)
                .epochSlot(epochSlot)
                .era(era)
                .build();
    }

}

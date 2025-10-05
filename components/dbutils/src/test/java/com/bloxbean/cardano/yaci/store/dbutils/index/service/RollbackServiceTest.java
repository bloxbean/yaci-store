package com.bloxbean.cardano.yaci.store.dbutils.index.service;

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
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.lang.reflect.Method;
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

    private RollbackLoader configLoader;
    private RollbackService rollbackService;

    @BeforeEach
    void setUp() {
        configLoader = new RollbackLoader();
        rollbackService = new RollbackService(jdbcTemplate, databaseUtils);
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
        when(databaseUtils.tableExists("cursor_")).thenReturn(true);
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
        when(databaseUtils.tableExists("cursor_")).thenReturn(true);
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
        when(databaseUtils.tableExists("cursor_")).thenReturn(true);
        when(jdbcTemplate.getJdbcTemplate()).thenReturn(plainJdbcTemplate);
        // Mock MAX(slot) query to return a slot that converts to epoch 200
        when(plainJdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(86400000L); // This should convert to epoch ~200

        boolean result = rollbackService.isValidRollbackEpoch(100, "MAINNET");

        assertTrue(result);
    }

    @Test
    void testGetRollbackBlockByEpochAndNetwork_WithValidEpoch_ShouldReturnBlock() throws Exception {
        when(databaseUtils.tableExists("cursor_")).thenReturn(true);
        
        // Mock the queryForObject to return a mock RollbackBlock
        RollbackBlock mockBlock = createMockRollbackBlock(100, 43200000L, "test-hash", 1000L, 0, 1);
        when(jdbcTemplate.queryForObject(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(mockBlock);

        Method method = RollbackService.class.getDeclaredMethod("getRollbackBlockByEpochAndNetwork", int.class, String.class);
        method.setAccessible(true);
        
        RollbackBlock result = (RollbackBlock) method.invoke(rollbackService, 100, "MAINNET");

        assertNotNull(result);
        assertEquals(100, result.getEpoch());
        assertEquals("test-hash", result.getHash());
        assertEquals(43200000L, result.getSlot());
        assertEquals(1000L, result.getNumber());
        assertEquals(1, result.getEra());
    }

    @Test
    void testGetRollbackBlockByEpochAndNetwork_WithCursorTableNotExists_ShouldReturnNull() throws Exception {
        when(databaseUtils.tableExists("cursor_")).thenReturn(false);

        Method method = RollbackService.class.getDeclaredMethod("getRollbackBlockByEpochAndNetwork", int.class, String.class);
        method.setAccessible(true);
        
        RollbackBlock result = (RollbackBlock) method.invoke(rollbackService, 100, "MAINNET");

        assertNull(result);
    }

    @Test
    void testGetRollbackBlockByEpochAndNetwork_WithDifferentNetworks() throws Exception {
        when(databaseUtils.tableExists("cursor_")).thenReturn(true);
        
        // Mock the queryForObject to return a mock RollbackBlock
        RollbackBlock mockBlock = createMockRollbackBlock(50, 21600000L, "test-hash", 500L, 0, 1);
        when(jdbcTemplate.queryForObject(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(mockBlock);

        Method method = RollbackService.class.getDeclaredMethod("getRollbackBlockByEpochAndNetwork", int.class, String.class);
        method.setAccessible(true);
        
        // Test with different networks
        RollbackBlock result1 = (RollbackBlock) method.invoke(rollbackService, 50, "PREPROD");
        RollbackBlock result2 = (RollbackBlock) method.invoke(rollbackService, 50, "PREVIEW");
        RollbackBlock result3 = (RollbackBlock) method.invoke(rollbackService, 50, "SANCHONET");

        assertNotNull(result1);
        assertNotNull(result2);
        assertNotNull(result3);
        assertEquals(50, result1.getEpoch());
        assertEquals(50, result2.getEpoch());
        assertEquals(50, result3.getEpoch());
    }

    @Test
    void testIsValidRollbackEpoch_WithInvalidEpoch_ShouldReturnFalse() {
        when(databaseUtils.tableExists("cursor_")).thenReturn(true);
        when(jdbcTemplate.getJdbcTemplate()).thenReturn(plainJdbcTemplate);
        // Mock MAX(slot) query to return a slot that converts to epoch 50 (much smaller than 100)
        when(plainJdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(2160000L); // This should convert to epoch ~50

        boolean result = rollbackService.isValidRollbackEpoch(101, "MAINNET");

        assertFalse(result);
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

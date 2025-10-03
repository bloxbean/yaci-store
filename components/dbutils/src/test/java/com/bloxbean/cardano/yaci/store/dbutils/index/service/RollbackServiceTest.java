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
        when(jdbcTemplate.getJdbcTemplate()).thenReturn(plainJdbcTemplate);
        when(plainJdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(200);

        boolean result = rollbackService.isValidRollbackEpoch(100);

        assertTrue(result);
    }

    @Test
    void testIsValidRollbackEpoch_WithInvalidEpoch_ShouldReturnFalse() {
        when(jdbcTemplate.getJdbcTemplate()).thenReturn(plainJdbcTemplate);
        when(plainJdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(50);

        boolean result = rollbackService.isValidRollbackEpoch(100);

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

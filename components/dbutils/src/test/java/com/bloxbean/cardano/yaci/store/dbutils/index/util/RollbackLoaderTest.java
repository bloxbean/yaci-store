package com.bloxbean.cardano.yaci.store.dbutils.index.util;

import com.bloxbean.cardano.yaci.store.dbutils.index.model.RollbackConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RollbackLoaderTest {

    private RollbackLoader configLoader;

    @BeforeEach
    void setUp() {
        configLoader = new RollbackLoader();
    }

    @Test
    void testLoadRollbackConfig_WithValidYaml_ShouldReturnCorrectConfig() {
        String yamlFilePath = "rollback.yml";

        RollbackConfig config = configLoader.loadRollbackConfig(yamlFilePath);

        assertNotNull(config);
        assertNotNull(config.getTables());
        assertTrue(config.getTables().size() > 0);

        RollbackConfig.TableRollbackDefinition epochStakeTable = findTableByName(config.getTables(), "epoch_stake");
        assertNotNull(epochStakeTable);
        assertEquals("DELETE", epochStakeTable.getOperation());
        assertEquals("epoch", epochStakeTable.getCondition().getType());
        assertEquals("epoch", epochStakeTable.getCondition().getColumn());
        assertEquals(">=", epochStakeTable.getCondition().getOperator());
        assertEquals(-1, epochStakeTable.getCondition().getOffset());

        RollbackConfig.TableRollbackDefinition adapotJobsTable = findTableByName(config.getTables(), "adapot_jobs");
        assertNotNull(adapotJobsTable);
        assertEquals("slot", adapotJobsTable.getCondition().getType());
        assertEquals("slot", adapotJobsTable.getCondition().getColumn());
        assertEquals(">", adapotJobsTable.getCondition().getOperator());

        RollbackConfig.TableRollbackDefinition txInputTable = findTableByName(config.getTables(), "tx_input");
        assertNotNull(txInputTable);
        assertEquals("DELETE", txInputTable.getOperation());
        assertEquals("slot", txInputTable.getCondition().getType());
        assertEquals("spent_at_slot", txInputTable.getCondition().getColumn());
        assertEquals(">", txInputTable.getCondition().getOperator());
        assertNull(txInputTable.getCondition().getOffset());

        RollbackConfig.TableRollbackDefinition assetTable = findTableByName(config.getTables(), "asset");
        assertNotNull(assetTable);
        assertEquals("DELETE", assetTable.getOperation());
        assertEquals("slot", assetTable.getCondition().getType());
        assertEquals("slot", assetTable.getCondition().getColumn());
        assertEquals(">", assetTable.getCondition().getOperator());
        assertNull(assetTable.getCondition().getOffset());
    }

    @Test
    void testLoadRollbackTableNames_WithValidYaml_ShouldReturnTableNames() {
        String yamlFilePath = "rollback.yml";

        List<String> tableNames = configLoader.loadRollbackTableNames(yamlFilePath);

        assertNotNull(tableNames);
        assertTrue(tableNames.size() > 0);
        assertTrue(tableNames.contains("epoch_stake"));
        assertTrue(tableNames.contains("adapot_jobs"));
        assertTrue(tableNames.contains("tx_input"));
        assertTrue(tableNames.contains("asset"));
    }

    @Test
    void testLoadRollbackConfigFromMultipleFiles_WithValidFiles_ShouldCombineConfigs() {
        String[] yamlFilePaths = {"test-rollback-1.yml", "test-rollback-2.yml"};

        RollbackConfig config = configLoader.loadRollbackConfigFromMultipleFiles(yamlFilePaths);

        assertNotNull(config);
        assertNotNull(config.getTables());
        assertEquals(15, config.getTables().size()); // 7 from test-rollback-1 + 8 from test-rollback-2

        // Verify tables from test-rollback-1.yml
        assertTrue(config.getTables().stream().anyMatch(table -> "asset".equals(table.getName())));
        assertTrue(config.getTables().stream().anyMatch(table -> "block".equals(table.getName())));
        assertTrue(config.getTables().stream().anyMatch(table -> "transaction".equals(table.getName())));
        assertTrue(config.getTables().stream().anyMatch(table -> "address_utxo".equals(table.getName())));
        assertTrue(config.getTables().stream().anyMatch(table -> "tx_input".equals(table.getName())));
        assertTrue(config.getTables().stream().anyMatch(table -> "adapot_jobs".equals(table.getName())));
        assertTrue(config.getTables().stream().anyMatch(table -> "epoch_stake".equals(table.getName())));

        // Verify tables from test-rollback-2.yml
        assertTrue(config.getTables().stream().anyMatch(table -> "pool".equals(table.getName())));
        assertTrue(config.getTables().stream().anyMatch(table -> "stake_registration".equals(table.getName())));
        assertTrue(config.getTables().stream().anyMatch(table -> "delegation".equals(table.getName())));
        assertTrue(config.getTables().stream().anyMatch(table -> "reward".equals(table.getName())));
        assertTrue(config.getTables().stream().anyMatch(table -> "drep_dist".equals(table.getName())));
        assertTrue(config.getTables().stream().anyMatch(table -> "gov_action_proposal_status".equals(table.getName())));
        assertTrue(config.getTables().stream().anyMatch(table -> "gov_epoch_activity".equals(table.getName())));
        assertTrue(config.getTables().stream().anyMatch(table -> "committee_state".equals(table.getName())));
    }

    @Test
    void testLoadRollbackConfigFromMultipleFiles_WithOneNonExistentFile_ShouldThrowException() {
        String[] filePaths = {"test-rollback-1.yml", "non-existent.yml"};

        assertThrows(IllegalArgumentException.class, () -> {
            configLoader.loadRollbackConfigFromMultipleFiles(filePaths);
        });
    }

    @Test
    void testLoadRollbackConfigFromMultipleFiles_WithMixedPaths_ShouldThrowException() {
        String[] filePaths = {"classpath:test-rollback-1.yml", "classpath:non-existent.yml", "test-rollback-2.yml"};

        assertThrows(IllegalArgumentException.class, () -> {
            configLoader.loadRollbackConfigFromMultipleFiles(filePaths);
        });
    }

    @Test
    void testLoadRollbackConfig_WithComplexUpdateSet_ShouldParseCorrectly() {
        String yamlFilePath = "test-rollback-1.yml";

        RollbackConfig config = configLoader.loadRollbackConfig(yamlFilePath);

        assertNotNull(config);
        RollbackConfig.TableRollbackDefinition adapotJobsTable = findTableByName(config.getTables(), "adapot_jobs");
        assertNotNull(adapotJobsTable);
        assertEquals("UPDATE", adapotJobsTable.getOperation());
        assertEquals(1, adapotJobsTable.getUpdateSet().size());

        RollbackConfig.TableRollbackDefinition.UpdateSet update1 = adapotJobsTable.getUpdateSet().get(0);
        assertEquals("status", update1.getColumn());
        assertEquals("'NOT_STARTED'", update1.getValue());
    }

    @Test
    void testLoadRollbackConfig_WithDifferentConditionTypes_ShouldParseCorrectly() {
        String yamlFilePath = "test-rollback-1.yml";

        RollbackConfig config = configLoader.loadRollbackConfig(yamlFilePath);

        assertNotNull(config);

        RollbackConfig.TableRollbackDefinition epochTable = findTableByName(config.getTables(), "epoch_stake");
        assertNotNull(epochTable);
        assertEquals("epoch", epochTable.getCondition().getType());
        assertEquals("epoch", epochTable.getCondition().getColumn());
        assertEquals(">=", epochTable.getCondition().getOperator());
        assertEquals(-1, epochTable.getCondition().getOffset());

        RollbackConfig.TableRollbackDefinition slotTable = findTableByName(config.getTables(), "asset");
        assertNotNull(slotTable);
        assertEquals("slot", slotTable.getCondition().getType());
        assertEquals("slot", slotTable.getCondition().getColumn());
        assertEquals(">", slotTable.getCondition().getOperator());
        assertNull(slotTable.getCondition().getOffset());
    }

    private RollbackConfig.TableRollbackDefinition findTableByName(List<RollbackConfig.TableRollbackDefinition> tables, String name) {
        return tables.stream()
                .filter(table -> name.equals(table.getName()))
                .findFirst()
                .orElse(null);
    }
}

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
        assertEquals("UPDATE", adapotJobsTable.getOperation());
        assertNotNull(adapotJobsTable.getUpdateSet());
        assertEquals(1, adapotJobsTable.getUpdateSet().size());
        assertEquals("status", adapotJobsTable.getUpdateSet().get(0).getColumn());
        assertEquals("'NOT_STARTED'", adapotJobsTable.getUpdateSet().get(0).getValue());

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

    private RollbackConfig.TableRollbackDefinition findTableByName(List<RollbackConfig.TableRollbackDefinition> tables, String name) {
        return tables.stream()
                .filter(table -> name.equals(table.getName()))
                .findFirst()
                .orElse(null);
    }
}

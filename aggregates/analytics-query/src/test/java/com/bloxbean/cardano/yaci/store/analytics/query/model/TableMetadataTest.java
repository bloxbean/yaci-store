package com.bloxbean.cardano.yaci.store.analytics.query.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TableMetadataTest {

    @Test
    void metadataMatchesExporterPartitionStrategies() {
        assertPartition("cost_model", "DAILY", "date");
        assertPartition("epoch_param", "EPOCH", "epoch");
        assertPartition("gov_epoch_activity", "EPOCH", "epoch");
        assertPartition("unclaimed_reward_rest", "EPOCH", "epoch");
    }

    @Test
    void metadataCoversPreviouslyMissingExporters() {
        assertOptionalPartition("address", "DAILY", "date");
        assertOptionalPartition("address_balance", "DAILY", "date");
        assertOptionalPartition("address_tx_amount", "DAILY", "date");
        assertOptionalPartition("epoch", "EPOCH", "epoch");
        assertOptionalPartition("transaction_witness", "DAILY", "date");

        TableMetadata addressBalance = TableMetadata.forTable("address_balance");
        assertTrue(addressBalance.queryHints().stream()
                .anyMatch(hint -> hint.contains("GET /addresses/{address}/amounts")
                        && hint.contains("when MCP is enabled")));
    }

    private static void assertPartition(String table, String strategy, String column) {
        TableMetadata metadata = TableMetadata.forTable(table);
        assertNotNull(metadata, table);
        assertEquals(strategy, metadata.partitionStrategy(), table);
        assertEquals(column, metadata.partitionColumn(), table);
    }

    private static void assertOptionalPartition(String table, String strategy, String column) {
        assertPartition(table, strategy, column);
        TableMetadata metadata = TableMetadata.forTable(table);
        assertTrue(metadata.description().startsWith("Optional"), table);
        assertTrue(metadata.queryHints().stream()
                .anyMatch(hint -> hint.contains("row count of zero means currently empty")), table);
        assertTrue(metadata.queryHints().stream()
                .anyMatch(hint -> hint.toLowerCase().contains("fallback")), table);
    }
}

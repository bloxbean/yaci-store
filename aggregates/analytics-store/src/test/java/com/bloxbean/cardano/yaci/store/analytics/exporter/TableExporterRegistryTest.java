package com.bloxbean.cardano.yaci.store.analytics.exporter;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TableExporterRegistryTest {

    @Test
    void registerExporter_shouldRegisterSuccessfully() {
        TableExporterRegistry registry = createRegistry();
        TableExporter exporter = mockExporter("my_custom_table", PartitionStrategy.DAILY);

        registry.registerExporter(exporter);

        assertThat(registry.hasExporter("my_custom_table")).isTrue();
        assertThat(registry.getExporter("my_custom_table")).isSameAs(exporter);
    }

    @Test
    void registerExporter_duplicate_shouldThrow() {
        TableExporterRegistry registry = createRegistry();
        TableExporter first = mockExporter("dup_table", PartitionStrategy.DAILY);
        TableExporter second = mockExporter("dup_table", PartitionStrategy.EPOCH);

        registry.registerExporter(first);

        assertThatThrownBy(() -> registry.registerExporter(second))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("dup_table");
    }

    @Test
    void registerExporter_shouldAppearInGetTablesByStrategy() {
        TableExporterRegistry registry = createRegistry();
        registry.registerExporter(mockExporter("daily_table", PartitionStrategy.DAILY));
        registry.registerExporter(mockExporter("epoch_table", PartitionStrategy.EPOCH));

        List<String> dailyTables = registry.getTablesByStrategy(PartitionStrategy.DAILY);
        List<String> epochTables = registry.getTablesByStrategy(PartitionStrategy.EPOCH);

        assertThat(dailyTables).containsExactly("daily_table");
        assertThat(epochTables).containsExactly("epoch_table");
    }

    private TableExporterRegistry createRegistry() {
        AnalyticsStoreProperties properties = new AnalyticsStoreProperties();
        return new TableExporterRegistry(properties);
    }

    private TableExporter mockExporter(String tableName, PartitionStrategy strategy) {
        TableExporter exporter = mock(TableExporter.class);
        when(exporter.getTableName()).thenReturn(tableName);
        when(exporter.getPartitionStrategy()).thenReturn(strategy);
        return exporter;
    }
}

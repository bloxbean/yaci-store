package com.bloxbean.cardano.yaci.store.analytics.config;

import com.bloxbean.cardano.yaci.store.adapot.job.storage.AdaPotJobStorage;
import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties.CustomExporterConfig;
import com.bloxbean.cardano.yaci.store.analytics.exporter.TableExporterRegistry;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateService;
import com.bloxbean.cardano.yaci.store.analytics.writer.StorageWriter;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomExporterRegistrarTest {

    @Test
    void registerCustomExporters_shouldRegisterIntoRegistry() {
        AnalyticsStoreProperties properties = new AnalyticsStoreProperties();
        properties.setCustomExporters(List.of(createConfig("custom_tx", "DAILY", "SELECT 1")));

        TableExporterRegistry registry = createRegistry();
        CustomExporterRegistrar registrar = createRegistrar(registry, properties);

        registrar.registerCustomExporters();

        assertThat(registry.hasExporter("custom_tx")).isTrue();
    }

    @Test
    void registerCustomExporters_shouldRegisterMultiple() {
        AnalyticsStoreProperties properties = new AnalyticsStoreProperties();
        properties.setCustomExporters(List.of(
                createConfig("table_a", "DAILY", "SELECT 1"),
                createConfig("table_b", "EPOCH", "SELECT 2")));

        TableExporterRegistry registry = createRegistry();
        CustomExporterRegistrar registrar = createRegistrar(registry, properties);

        registrar.registerCustomExporters();

        assertThat(registry.hasExporter("table_a")).isTrue();
        assertThat(registry.hasExporter("table_b")).isTrue();
        assertThat(registry.getExporterCount()).isEqualTo(2);
    }

    @Test
    void registerCustomExporters_emptyList_shouldBeNoop() {
        AnalyticsStoreProperties properties = new AnalyticsStoreProperties();
        properties.setCustomExporters(new ArrayList<>());

        TableExporterRegistry registry = createRegistry();
        CustomExporterRegistrar registrar = createRegistrar(registry, properties);

        registrar.registerCustomExporters();

        assertThat(registry.getExporterCount()).isEqualTo(0);
    }

    @Test
    void registerCustomExporters_nullList_shouldBeNoop() {
        AnalyticsStoreProperties properties = new AnalyticsStoreProperties();
        properties.setCustomExporters(null);

        TableExporterRegistry registry = createRegistry();
        CustomExporterRegistrar registrar = createRegistrar(registry, properties);

        registrar.registerCustomExporters();

        assertThat(registry.getExporterCount()).isEqualTo(0);
    }

    @Test
    void registerCustomExporters_emptyName_shouldThrow() {
        AnalyticsStoreProperties properties = new AnalyticsStoreProperties();
        properties.setCustomExporters(List.of(createConfig("", "DAILY", "SELECT 1")));

        TableExporterRegistry registry = createRegistry();
        CustomExporterRegistrar registrar = createRegistrar(registry, properties);

        assertThatThrownBy(registrar::registerCustomExporters)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }

    @Test
    void registerCustomExporters_emptyQuery_shouldThrow() {
        AnalyticsStoreProperties properties = new AnalyticsStoreProperties();
        properties.setCustomExporters(List.of(createConfig("my_table", "DAILY", "")));

        TableExporterRegistry registry = createRegistry();
        CustomExporterRegistrar registrar = createRegistrar(registry, properties);

        assertThatThrownBy(registrar::registerCustomExporters)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("query");
    }

    @Test
    void registerCustomExporters_duplicateName_shouldThrow() {
        AnalyticsStoreProperties properties = new AnalyticsStoreProperties();
        properties.setCustomExporters(List.of(
                createConfig("dup_table", "DAILY", "SELECT 1"),
                createConfig("dup_table", "EPOCH", "SELECT 2")));

        TableExporterRegistry registry = createRegistry();
        CustomExporterRegistrar registrar = createRegistrar(registry, properties);

        assertThatThrownBy(registrar::registerCustomExporters)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("dup_table");
    }

    private CustomExporterConfig createConfig(String name, String strategy, String query) {
        CustomExporterConfig config = new CustomExporterConfig();
        config.setName(name);
        config.setPartitionStrategy(strategy);
        config.setQuery(query);
        return config;
    }

    private TableExporterRegistry createRegistry() {
        AnalyticsStoreProperties registryProps = new AnalyticsStoreProperties();
        return new TableExporterRegistry(registryProps);
    }

    private CustomExporterRegistrar createRegistrar(TableExporterRegistry registry, AnalyticsStoreProperties properties) {
        StorageWriter storageWriter = mock(StorageWriter.class);
        when(storageWriter.getSourceSchema()).thenReturn("mainnet");

        ExportStateService stateService = mock(ExportStateService.class);
        EraService eraService = mock(EraService.class);
        AdaPotJobStorage adaPotJobStorage = mock(AdaPotJobStorage.class);

        return new CustomExporterRegistrar(registry, storageWriter, stateService, eraService, properties, adaPotJobStorage);
    }
}

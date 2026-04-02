package com.bloxbean.cardano.yaci.store.analytics.config;

import com.bloxbean.cardano.yaci.store.adapot.job.storage.AdaPotJobStorage;
import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties.CustomExporterConfig;
import com.bloxbean.cardano.yaci.store.analytics.exporter.CustomTableExporter;
import com.bloxbean.cardano.yaci.store.analytics.exporter.TableExporterRegistry;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateService;
import com.bloxbean.cardano.yaci.store.analytics.writer.StorageWriter;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class CustomExporterRegistrar {

    private final TableExporterRegistry registry;
    private final StorageWriter storageWriter;
    private final ExportStateService stateService;
    private final EraService eraService;
    private final AnalyticsStoreProperties properties;
    private final AdaPotJobStorage adaPotJobStorage;

    @PostConstruct
    public void registerCustomExporters() {
        List<CustomExporterConfig> configs = properties.getCustomExporters();
        if (configs == null || configs.isEmpty()) return;

        for (CustomExporterConfig config : configs) {
            validateConfig(config);

            CustomTableExporter exporter = new CustomTableExporter(
                    config, storageWriter, stateService, eraService, properties, adaPotJobStorage);
            registry.registerExporter(exporter);

            log.info("Registered custom exporter: {} (strategy: {})",
                    config.getName(), config.getPartitionStrategy());
        }

        log.info("Registered {} custom exporter(s)", configs.size());
    }

    private void validateConfig(CustomExporterConfig config) {
        if (config.getName() == null || config.getName().isBlank()) {
            throw new IllegalArgumentException("Custom exporter 'name' must not be empty");
        }
        if (config.getQuery() == null || config.getQuery().isBlank()) {
            throw new IllegalArgumentException(
                    "Custom exporter 'query' must not be empty for: " + config.getName());
        }
    }
}

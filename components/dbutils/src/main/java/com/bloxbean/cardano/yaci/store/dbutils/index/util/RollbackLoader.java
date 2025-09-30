package com.bloxbean.cardano.yaci.store.dbutils.index.util;

import com.bloxbean.cardano.yaci.store.dbutils.index.model.RollbackConfig;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RollbackLoader {

    private ResourceLoader resourceLoader;

    public RollbackLoader() {
        this.resourceLoader = new DefaultResourceLoader();
    }

    public RollbackLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @SuppressWarnings("unchecked")
    public List<String> loadRollbackTableNames(String yamlFilePath) {
        RollbackConfig config = loadRollbackConfig(yamlFilePath);
        List<String> tables = new ArrayList<>();
        
        for (RollbackConfig.TableRollbackDefinition tableDef : config.getTables()) {
            tables.add(tableDef.getName());
        }
        
        return tables;
    }

    @SuppressWarnings("unchecked")
    public RollbackConfig loadRollbackConfig(String yamlFilePath) {
        Yaml yaml = new Yaml();

        InputStream is = getClass().getClassLoader().getResourceAsStream(yamlFilePath);
        if (is == null) {
            throw new IllegalArgumentException("File not found: " + yamlFilePath);
        }

        Map<String, Object> root = yaml.load(is);
        return parseRollbackConfig(root);
    }

    @SuppressWarnings("unchecked")
    private RollbackConfig parseRollbackConfig(Map<String, Object> root) {
        Map<String, Object> rollbackData = (Map<String, Object>) root.get("rollback");
        if (rollbackData == null) {
            throw new IllegalArgumentException("Invalid rollback config: missing 'rollback' section");
        }

        // Parse tables section
        List<Map<String, Object>> tablesData = (List<Map<String, Object>>) rollbackData.get("tables");
        List<RollbackConfig.TableRollbackDefinition> tables = new ArrayList<>();

        if (tablesData != null) {
            for (Map<String, Object> tableData : tablesData) {
                RollbackConfig.TableRollbackDefinition table = parseTableDefinition(tableData);
                tables.add(table);
            }
        }

        return RollbackConfig.builder()
                .tables(tables)
                .build();
    }

    @SuppressWarnings("unchecked")
    private RollbackConfig.TableRollbackDefinition parseTableDefinition(Map<String, Object> tableData) {
        String name = (String) tableData.get("name");
        String operation = (String) tableData.get("operation");

        // Parse condition
        Map<String, Object> conditionData = (Map<String, Object>) tableData.get("condition");
        RollbackConfig.TableRollbackDefinition.Condition condition = null;
        if (conditionData != null) {
            condition = RollbackConfig.TableRollbackDefinition.Condition.builder()
                    .type((String) conditionData.get("type"))
                    .column((String) conditionData.get("column"))
                    .operator((String) conditionData.get("operator"))
                    .offset((Integer) conditionData.get("offset"))
                    .build();
        }

        // Parse update_set for UPDATE operations
        List<Map<String, Object>> updateSetData = (List<Map<String, Object>>) tableData.get("update_set");
        List<RollbackConfig.TableRollbackDefinition.UpdateSet> updateSet = null;
        if (updateSetData != null) {
            updateSet = new ArrayList<>();
            for (Map<String, Object> updateData : updateSetData) {
                RollbackConfig.TableRollbackDefinition.UpdateSet update =
                    RollbackConfig.TableRollbackDefinition.UpdateSet.builder()
                        .column((String) updateData.get("column"))
                        .value((String) updateData.get("value"))
                        .build();
                updateSet.add(update);
            }
        }

        return RollbackConfig.TableRollbackDefinition.builder()
                .name(name)
                .operation(operation)
                .condition(condition)
                .updateSet(updateSet)
                .build();
    }

    /**
     * Load rollback configuration from multiple YAML files
     * Supports both classpath resources and external files via Spring ResourceLoader
     * @param filePaths Array of file paths (e.g., "rollback.yml", "classpath:custom-rollback.yml", "file:///path/to/file.yml")
     * @return Combined RollbackConfig with all tables from all files
     */
    public RollbackConfig loadRollbackConfigFromMultipleFiles(String... filePaths) {
        List<RollbackConfig.TableRollbackDefinition> allTables = new ArrayList<>();

        for (String filePath : filePaths) {
            RollbackConfig config;

            // Check if it's a resource path (classpath:, file:, etc.)
            if (isResourcePath(filePath)) {
                if (resourceLoader == null) {
                    throw new IllegalStateException("ResourceLoader not configured for resource path: " + filePath);
                }
                config = loadRollbackConfigFromResourcePath(filePath);
            } else {
                // Regular classpath resource
                config = loadRollbackConfig(filePath);
            }

            if (config != null && config.getTables() != null) {
                allTables.addAll(config.getTables());
            }
        }

        return RollbackConfig.builder()
                .tables(allTables)
                .build();
    }

    private boolean isResourcePath(String filePath) {
        return filePath.startsWith("classpath:") || 
               filePath.startsWith("file:") || 
               filePath.startsWith("http:") || 
               filePath.startsWith("https:");
    }

    private RollbackConfig loadRollbackConfigFromResourcePath(String resourcePath) {
        try {
            Resource resource = resourceLoader.getResource(resourcePath);
            if (!resource.exists()) {
                throw new IllegalArgumentException("Resource not found: " + resourcePath);
            }

            Yaml yaml = new Yaml();
            Map<String, Object> root = yaml.load(resource.getInputStream());
            return parseRollbackConfig(root);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error loading resource: " + resourcePath, e);
        }
    }
}

package com.bloxbean.cardano.yaci.store.dbutils.index.util;

import com.bloxbean.cardano.yaci.store.dbutils.index.model.IndexDefinition;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IndexLoader {

    private ResourceLoader resourceLoader;

    public IndexLoader() {
        this.resourceLoader = new DefaultResourceLoader();
    }

    public IndexLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public List<IndexDefinition> loadIndexes(String yamlFilePath) {
        Yaml yaml = new Yaml();
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(yamlFilePath);
        if (inputStream == null) {
            throw new IllegalArgumentException("File not found: " + yamlFilePath);
        }

        List<Map<String, Object>> data = yaml.load(inputStream);
        return parseIndexDefinitions(data);
    }

    public List<IndexDefinition> loadIndexesFromMultipleFiles(String... filePaths) {
        List<IndexDefinition> allIndexDefinitions = new ArrayList<>();

        for (String filePath : filePaths) {
            String normalizedFilePath = normalizeFilePath(filePath);
            List<IndexDefinition> indexDefinitions;

            if (isResourcePath(normalizedFilePath)) {
                if (resourceLoader == null) {
                    throw new IllegalStateException("ResourceLoader not configured for resource path: " + normalizedFilePath);
                }
                indexDefinitions = loadIndexesFromResourcePath(normalizedFilePath);
            } else {
                indexDefinitions = loadIndexes(normalizedFilePath);
            }

            if (indexDefinitions != null) {
                allIndexDefinitions.addAll(indexDefinitions);
            }
        }

        return allIndexDefinitions;
    }

    private boolean isResourcePath(String filePath) {
        return filePath.startsWith("classpath:") ||
                filePath.startsWith("file:") ||
                filePath.startsWith("http:") ||
                filePath.startsWith("https:");
    }

    private List<IndexDefinition> loadIndexesFromResourcePath(String resourcePath) {
        try {
            Resource resource = resourceLoader.getResource(resourcePath);
            if (!resource.exists()) {
                throw new IllegalArgumentException("Resource not found: " + resourcePath);
            }

            Yaml yaml = new Yaml();
            try (InputStream inputStream = resource.getInputStream()) {
                List<Map<String, Object>> data = yaml.load(inputStream);
                return parseIndexDefinitions(data);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Error loading resource: " + resourcePath, e);
        }
    }

    private String normalizeFilePath(String filePath) {
        if (filePath == null) {
            throw new IllegalArgumentException("Index file path cannot be null");
        }

        String normalizedFilePath = filePath.trim();
        if (normalizedFilePath.isEmpty()) {
            throw new IllegalArgumentException("Index file path cannot be empty");
        }

        if (isResourcePath(normalizedFilePath)) {
            return normalizedFilePath;
        }

        String fileName = normalizedFilePath;
        int lastSeparator = Math.max(normalizedFilePath.lastIndexOf('/'), normalizedFilePath.lastIndexOf('\\'));
        if (lastSeparator >= 0) {
            fileName = normalizedFilePath.substring(lastSeparator + 1);
        }

        if (!fileName.contains(".")) {
            return normalizedFilePath + ".yml";
        }

        return normalizedFilePath;
    }

    @SuppressWarnings("unchecked")
    private List<IndexDefinition> parseIndexDefinitions(List<Map<String, Object>> data) {
        if (data == null) {
            return new ArrayList<>();
        }

        List<IndexDefinition> indexDefinitions = new ArrayList<>();
        for (Map<String, Object> item : data) {
            String table = (String) item.get("table");
            List<Map<String, Object>> indexesData = (List<Map<String, Object>>) item.get("indexes");

            List<IndexDefinition.Index> indexes = new ArrayList<>();
            for (Map<String, Object> indexMap : indexesData) {
                String name = (String) indexMap.get("name");
                List<String> columns = (List<String>) indexMap.get("columns");
                String type = (String) indexMap.get("type");
                List<String> excludes = (List<String>) indexMap.get("excludes");
                String where = (String) indexMap.get("where");
                IndexDefinition.Index index = new IndexDefinition.Index(name, columns, type, excludes, where);
                indexes.add(index);
            }

            IndexDefinition indexDef = new IndexDefinition(table, indexes);
            indexDefinitions.add(indexDef);
        }

        return indexDefinitions;
    }
}

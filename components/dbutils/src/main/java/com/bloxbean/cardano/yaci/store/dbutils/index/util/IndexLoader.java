package com.bloxbean.cardano.yaci.store.dbutils.index.util;

import com.bloxbean.cardano.yaci.store.dbutils.index.model.IndexDefinition;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IndexLoader {

    public List<IndexDefinition> loadIndexes(String yamlFilePath) {
        Yaml yaml = new Yaml();
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(yamlFilePath);
        if (inputStream == null) {
            throw new IllegalArgumentException("File not found: " + yamlFilePath);
        }

        List<Map<String, Object>> data = yaml.load(inputStream);

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
                IndexDefinition.Index index = new IndexDefinition.Index(name, columns, type, excludes);
                indexes.add(index);
            }

            IndexDefinition indexDef = new IndexDefinition(table, indexes);
            indexDefinitions.add(indexDef);
        }

        return indexDefinitions;
    }
}

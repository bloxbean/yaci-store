package com.bloxbean.cardano.yaci.store.dbutils.index.util;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RollbackLoader {

    @SuppressWarnings("unchecked")
    public List<String> loadRollbackTableNames(String yamlFilePath) {
        Yaml yaml = new Yaml();

        InputStream is = getClass().getClassLoader().getResourceAsStream(yamlFilePath);
        if (is == null) {
            throw new IllegalArgumentException("File not found: " + yamlFilePath);
        }

        List<Map<String, Object>> data = yaml.load(is);
        List<String> tables = new ArrayList<>();

        for (Map<String, Object> item : data) {
            String table = (String) item.get("table");
            if (table != null && !table.trim().isEmpty()) {
                tables.add(table);
            }
        }
        return tables;
    }
}

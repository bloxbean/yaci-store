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

        Map<String, Object> root = yaml.load(is);
        List<String> tables = new ArrayList<>();

        Object tablesObj = root.get("tables");
        if (tablesObj instanceof List) {
            for (Object t : (List<Object>) tablesObj) {
                if (t instanceof String) {
                    String table = ((String) t).trim();
                    if (!table.isEmpty()) {
                        tables.add(table);
                    }
                }
            }
        }

        return tables;
    }
}

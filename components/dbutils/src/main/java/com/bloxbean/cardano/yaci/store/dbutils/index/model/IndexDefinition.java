package com.bloxbean.cardano.yaci.store.dbutils.index.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IndexDefinition {
    private String table;
    private List<Index> indexes;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Index {
        private String name;
        private List<String> columns;
        private String type;
        private List<String> excludes;
    }
}

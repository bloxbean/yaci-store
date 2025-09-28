package com.bloxbean.cardano.yaci.store.dbutils.index.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RollbackConfig {
    private List<TableRollbackDefinition> tables;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TableRollbackDefinition {
        private String name;
        private String operation;
        private Condition condition;
        private List<UpdateSet> updateSet;
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Condition {
            private String type;
            private String column;
            private String operator;
            private Integer offset;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class UpdateSet {
            private String column;
            private String value;
        }
    }
}

package com.bloxbean.cardano.yaci.store.dbutils.index.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TableRollbackAction {
    private String tableName;
    private String sql;
}

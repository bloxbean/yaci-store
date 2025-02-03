package com.bloxbean.cardano.yaci.store.dbutils.index.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TableIndex {
    private String tableName;
    private String index;
}

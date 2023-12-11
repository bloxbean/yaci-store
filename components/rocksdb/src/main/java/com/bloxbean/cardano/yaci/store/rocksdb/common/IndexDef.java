package com.bloxbean.cardano.yaci.store.rocksdb.common;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.function.Function;

@AllArgsConstructor
@Data
public class IndexDef<V>  {
    private String indexName;
    private Function<V, List<IndexRecord>> keyMapper;
}

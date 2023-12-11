package com.bloxbean.cardano.yaci.store.rocksdb.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IndexRecord<V> {
    private String partKey;
    private String secondaryKey;
    private V value;

    public IndexRecord(String partKey, String secondaryKey) {
        this.partKey = partKey;
        this.secondaryKey = secondaryKey;
    }
}

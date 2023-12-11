package com.bloxbean.cardano.yaci.store.rocksdb.types;

import lombok.Data;

@Data
public class ListMetadata {
    private long size;
    private byte[] head;
    private byte[] tail;
}

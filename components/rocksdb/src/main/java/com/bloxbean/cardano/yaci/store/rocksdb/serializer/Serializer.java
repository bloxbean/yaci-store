package com.bloxbean.cardano.yaci.store.rocksdb.serializer;

public interface Serializer {
    byte[] serialize(Object obj);
    <T> T deserialize(byte[] bytes, Class<T> clazz  );
}

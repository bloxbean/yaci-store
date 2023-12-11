package com.bloxbean.cardano.yaci.store.rocksdb.serializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.msgpack.jackson.dataformat.MessagePackMapper;

public class MessagePackSerializer implements Serializer {
    private ObjectMapper objectMapper;

    public MessagePackSerializer() {
        this.objectMapper = new MessagePackMapper().handleBigDecimalAsString();
    }

    @SneakyThrows
    @Override
    public byte[] serialize(Object obj) {
        if (obj instanceof String s) {
            return s.getBytes();
        } else {
            return objectMapper.writeValueAsBytes(obj);
        }
    }

    @SneakyThrows
    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz  ) {
        if (clazz == String.class) {
            return (T) new String(bytes);
        } else {
            return objectMapper.readValue(bytes, clazz);
        }
    }
}

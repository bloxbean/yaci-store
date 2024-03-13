package com.bloxbean.cardano.yaci.store.rocksdb;

import lombok.Data;

import java.nio.ByteBuffer;
import java.util.Base64;

@Data
public class KeyReference {
    private byte[] ns;
    private byte[] key;

    public KeyReference(byte[] ns, byte[] key) {
        this.ns = ns;
        this.key = key;
    }

    public byte[] getNs() {
        return ns;
    }

    public byte[] getKey() {
        return key;
    }

    // Serialize the KeyReference object to a byte array
    public byte[] serialize() {
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + (ns != null ? ns.length : 0) +
                Integer.BYTES + (key != null ? key.length : 0));

        if (ns != null) {
            buffer.putInt(ns.length);
            buffer.put(ns);
        } else {
            buffer.putInt(0);
        }

        if (key != null) {
            buffer.putInt(key.length);
            buffer.put(key);
        } else {
            buffer.putInt(0);
        }

        return Base64.getEncoder().encode(buffer.array());
    }

    // Deserialize a byte array to a KeyReference object
    public static KeyReference deserialize(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(Base64.getDecoder().decode(data));

        int nsLength = buffer.getInt();
        byte[] ns = new byte[nsLength];
        if (nsLength > 0) {
            buffer.get(ns);
        }

        int keyLength = buffer.getInt();
        byte[] key = new byte[keyLength];
        if (keyLength > 0) {
            buffer.get(key);
        }

        return new KeyReference(nsLength > 0 ? ns : null, keyLength > 0 ? key : null);
    }
}

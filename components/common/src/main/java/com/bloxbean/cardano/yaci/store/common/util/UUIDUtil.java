package com.bloxbean.cardano.yaci.store.common.util;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public class UUIDUtil {
    private static final UUID NAMESPACE = UUID.fromString("1cd62603-57e9-4254-a1a4-b1ae636cf00e");

    /**
     * Name based UUID (RFC 4122 version 5) of the given parts, so a row derived from the same chain data
     * always gets the same id. Each part is length prefixed and null is encoded as length -1.
     */
    public static UUID nameUUID(String... parts) {
        MessageDigest sha1;
        try {
            sha1 = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }

        sha1.update(toBytes(NAMESPACE));
        for (String part : parts) {
            if (part == null) {
                sha1.update(ByteBuffer.allocate(4).putInt(-1).array());
            } else {
                byte[] bytes = part.getBytes(StandardCharsets.UTF_8);
                sha1.update(ByteBuffer.allocate(4).putInt(bytes.length).array());
                sha1.update(bytes);
            }
        }

        byte[] hash = sha1.digest();
        hash[6] = (byte) ((hash[6] & 0x0f) | 0x50); //version 5
        hash[8] = (byte) ((hash[8] & 0x3f) | 0x80); //IETF variant
        ByteBuffer buffer = ByteBuffer.wrap(hash, 0, 16);
        return new UUID(buffer.getLong(), buffer.getLong());
    }

    /**
     * jOOQ binds UUID as a string on MySQL, where the id column is binary(16) as written by Hibernate.
     */
    public static Field<UUID> idField(DSLContext dsl, Field<UUID> idField) {
        if (dsl.dialect().family() != SQLDialect.MYSQL)
            return idField;

        return DSL.field(idField.getUnqualifiedName(),
                SQLDataType.BINARY(16).asConvertedDataType(UUID.class, UUIDUtil::fromBytes, UUIDUtil::toBytes));
    }

    private static byte[] toBytes(UUID uuid) {
        return ByteBuffer.allocate(16)
                .putLong(uuid.getMostSignificantBits())
                .putLong(uuid.getLeastSignificantBits())
                .array();
    }

    private static UUID fromBytes(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        return new UUID(buffer.getLong(), buffer.getLong());
    }
}

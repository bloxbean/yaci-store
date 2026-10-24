package com.bloxbean.cardano.yaci.store.common.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import io.hypersistence.utils.hibernate.type.util.JsonSerializer;
import io.hypersistence.utils.hibernate.type.util.ObjectMapperWrapper;
import org.hibernate.internal.util.SerializationHelper;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Copies the value of a {@code @Type(JsonType.class)} entity attribute for Hibernate's dirty checking.
 * <p>
 * hypersistence-utils 3.9+ only copies {@link Serializable} values and fails on others (for example the epoch
 * nonce's {@code ProtocolParams} and {@code Vrf}). Yaci Store's JSON attributes were written for 3.7, which copied
 * other values through a JSON round trip. This restores that behaviour, using the same Jackson 2 mapper that writes
 * the column, so the stored JSON is unchanged. Registered in {@code hypersistence-utils.properties}.
 */
public class JsonColumnCloner implements JsonSerializer {
    private static final ObjectMapperWrapper MAPPER = new ObjectMapperWrapper();

    @Override
    @SuppressWarnings("unchecked")
    public <T> T clone(T object) {
        if (object == null || object instanceof String)
            return object;
        if (object instanceof JsonNode jsonNode)
            return (T) jsonNode.deepCopy();
        if (object instanceof Optional<?> optional)
            return (T) optional.map(this::clone);

        if (object instanceof Collection<?> collection) {
            Object element = collection.stream().filter(e -> e != null).findFirst().orElse(null);
            if (element != null && !(element instanceof Serializable))
                return jsonClone(object, TypeFactory.defaultInstance()
                        .constructParametricType(object.getClass(), element.getClass()));
        } else if (object instanceof Map<?, ?> map) {
            Map.Entry<?, ?> entry = map.entrySet().stream()
                    .filter(e -> e.getKey() != null && e.getValue() != null).findFirst().orElse(null);
            if (entry != null && (!(entry.getKey() instanceof Serializable) || !(entry.getValue() instanceof Serializable)))
                return jsonClone(object, TypeFactory.defaultInstance().constructParametricType(object.getClass(),
                        entry.getKey().getClass(), entry.getValue().getClass()));
        }

        if (object instanceof Serializable serializable) {
            try {
                return (T) SerializationHelper.clone(serializable);
            } catch (RuntimeException e) {
                //Serializable on the outside but not all the way down: fall back to JSON
            }
        }

        return jsonClone(object, TypeFactory.defaultInstance().constructType(object.getClass()));
    }

    private static <T> T jsonClone(T object, JavaType type) {
        return MAPPER.fromBytes(MAPPER.toBytes(object), type);
    }
}

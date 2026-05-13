package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.util;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.model.Cip26Metadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.model.Item;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.model.Mapping;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Function;

@Slf4j
public final class MappingsUtil {

    private MappingsUtil() {
    }

    public static Cip26Metadata toCip26Metadata(Mapping mapping, String updateBy, LocalDateTime updatedAt) {
        Cip26Metadata cip26Metadata = new Cip26Metadata();
        cip26Metadata.setSubject(mapping.subject());
        cip26Metadata.setPolicy(mapping.policy());
        cip26Metadata.setName(getValue(mapping.name()));
        cip26Metadata.setTicker(getValue(mapping.ticker()));
        cip26Metadata.setUrl(getValue(mapping.url()));
        cip26Metadata.setDescription(getValue(mapping.description()));
        cip26Metadata.setDecimals(getValue(mapping.decimals(), s -> {
            try {
                return Long.valueOf(s);
            } catch (NumberFormatException e) {
                log.warn("Invalid decimals value '{}' for subject '{}'", s, mapping.subject());
                return null;
            }
        }));
        cip26Metadata.setUpdated(updatedAt);
        cip26Metadata.setUpdatedBy(updateBy);
        cip26Metadata.setProperties(mapping);
        return cip26Metadata;
    }

    /** Extract the logo's base64 string from a Mapping, or null if absent. */
    public static String extractLogo(Mapping mapping) {
        return getValue(mapping.logo());
    }

    private static String getValue(Item item) {
        return getValue(item, Function.identity());
    }

    private static <T> T getValue(Item item, Function<String, T> f) {
        return Optional.ofNullable(item).map(Item::value).map(f).orElse(null);
    }
}

package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.util;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.model.TokenLogo;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.model.TokenMetadata;
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

    public static TokenMetadata toTokenMetadata(Mapping mapping, String updateBy, LocalDateTime updatedAt) {
        TokenMetadata tokenMetadata = new TokenMetadata();
        tokenMetadata.setSubject(mapping.subject());
        tokenMetadata.setPolicy(mapping.policy());
        tokenMetadata.setName(getValue(mapping.name()));
        tokenMetadata.setTicker(getValue(mapping.ticker()));
        tokenMetadata.setUrl(getValue(mapping.url()));
        tokenMetadata.setDescription(getValue(mapping.description()));
        tokenMetadata.setDecimals(getValue(mapping.decimals(), s -> {
            try {
                return Long.valueOf(s);
            } catch (NumberFormatException e) {
                log.warn("Invalid decimals value '{}' for subject '{}'", s, mapping.subject());
                return null;
            }
        }));
        tokenMetadata.setUpdated(updatedAt);
        tokenMetadata.setUpdatedBy(updateBy);
        tokenMetadata.setProperties(mapping);
        return tokenMetadata;
    }

    public static TokenLogo toTokenLogo(Mapping mapping) {
        TokenLogo tokenLogo = new TokenLogo();
        tokenLogo.setSubject(mapping.subject());
        tokenLogo.setLogo(getValue(mapping.logo()));
        return tokenLogo;
    }


    private static String getValue(Item item) {
        return getValue(item, Function.identity());
    }

    private static <T> T getValue(Item item, Function<String, T> f) {
        return Optional.ofNullable(item).map(Item::value).map(f).orElse(null);
    }


}

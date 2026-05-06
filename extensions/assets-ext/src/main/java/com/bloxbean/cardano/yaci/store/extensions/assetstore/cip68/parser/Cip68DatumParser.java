package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.parser;

import com.bloxbean.cardano.client.plutus.spec.*;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.ParsedCip68Datum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class Cip68DatumParser {

    // Well-known CIP-68 keys that map to typed columns on cip68_metadata.
    public static final String DECIMALS    = "decimals";
    public static final String DESCRIPTION = "description";
    public static final String LOGO        = "logo";
    public static final String NAME        = "name";
    public static final String TICKER      = "ticker";
    public static final String URL         = "url";
    public static final String IMAGE       = "image";
    public static final String MEDIA_TYPE  = "mediaType";
    public static final String FILES       = "files";

    /** Set of keys we promote to typed columns; everything else goes into the JSONB additional_properties. */
    private static final Set<String> TYPED_KEYS = Set.of(
            DECIMALS, DESCRIPTION, LOGO, NAME, TICKER, URL, IMAGE, MEDIA_TYPE, FILES);

    /**
     * Parse a CIP-68 reference NFT inline datum into the richer {@link ParsedCip68Datum}.
     * <p>
     * Extracts the FT-shape scalars (name, description, ticker, decimals, url, logo, version)
     * AND the NFT-shape scalars (image, mediaType), AND the variable-shape parts (files[],
     * arbitrary additional properties) into the JSONB-bound {@code properties} map.
     */
    public Optional<ParsedCip68Datum> parse(String inlineDatum) {
        if (inlineDatum == null || inlineDatum.isBlank()) {
            return Optional.empty();
        }

        try {
            PlutusData plutusData = PlutusData.deserialize(HexUtil.decodeHexString(inlineDatum));

            if (!(plutusData instanceof ConstrPlutusData cip68Data)) {
                return Optional.empty();
            }

            List<PlutusData> dataList = cip68Data.getData().getPlutusDataList();
            if (dataList.size() < 2 || !(dataList.getFirst() instanceof MapPlutusData properties)) {
                return Optional.empty();
            }

            if (!(dataList.get(1) instanceof BigIntPlutusData version)) {
                return Optional.empty();
            }

            // Typed scalars
            Long    decimals    = getNumericProperty(DECIMALS, properties).orElse(null);
            String  description = getStringProperty(DESCRIPTION, properties).orElse(null);
            String  logo        = getStringProperty(LOGO, properties).orElse(null);
            String  name        = getStringProperty(NAME, properties).orElse(null);
            String  ticker      = getStringProperty(TICKER, properties).orElse(null);
            String  url         = getStringProperty(URL, properties).orElse(null);
            String  image       = getStringOrChunkedProperty(IMAGE, properties).orElse(null);
            String  mediaType   = getStringProperty(MEDIA_TYPE, properties).orElse(null);

            // files[] and additional_properties go into a single JSONB column
            List<Map<String, Object>> files = parseFiles(properties);
            Map<String, Object> additional = parseAdditionalProperties(properties);

            Map<String, Object> propertiesJson = null;
            if ((files != null && !files.isEmpty()) || (additional != null && !additional.isEmpty())) {
                propertiesJson = new LinkedHashMap<>();
                if (files != null && !files.isEmpty()) {
                    propertiesJson.put("files", files);
                }
                if (additional != null && !additional.isEmpty()) {
                    propertiesJson.put("additional_properties", additional);
                }
            }

            return Optional.of(new ParsedCip68Datum(
                    decimals, description, logo, name, ticker, url, version.getValue().longValue(),
                    image, mediaType, propertiesJson));

        } catch (Exception e) {
            log.warn("Unexpected error while parsing CIP-68 datum: {}", inlineDatum, e);
            return Optional.empty();
        }
    }

    private Optional<String> getStringProperty(String propertyName, MapPlutusData mapPlutusData) {
        PlutusData property = mapPlutusData.getMap().get(BytesPlutusData.of(propertyName));
        return switch (property) {
            case BytesPlutusData bytes -> Optional.of(bytesToString(bytes.getValue()));
            case null, default -> Optional.empty();
        };
    }

    /**
     * CIP-25 convention (inherited by CIP-68 NFTs): if a string value exceeds 64 bytes
     * the issuer may split it into a list of byte-string chunks. This helper joins them
     * back together. Falls back to {@link #getStringProperty} for the simple-string case.
     */
    private Optional<String> getStringOrChunkedProperty(String propertyName, MapPlutusData mapPlutusData) {
        PlutusData property = mapPlutusData.getMap().get(BytesPlutusData.of(propertyName));
        return switch (property) {
            case BytesPlutusData bytes -> Optional.of(bytesToString(bytes.getValue()));
            case ListPlutusData list -> {
                StringBuilder sb = new StringBuilder();
                for (PlutusData chunk : list.getPlutusDataList()) {
                    if (chunk instanceof BytesPlutusData b) {
                        sb.append(bytesToString(b.getValue()));
                    }
                }
                yield sb.length() == 0 ? Optional.empty() : Optional.of(sb.toString());
            }
            case null, default -> Optional.empty();
        };
    }

    private Optional<Long> getNumericProperty(String propertyName, MapPlutusData mapPlutusData) {
        PlutusData property = mapPlutusData.getMap().get(BytesPlutusData.of(propertyName));
        return switch (property) {
            case BigIntPlutusData bigInt -> Optional.of(bigInt.getValue().longValue());
            case null, default -> Optional.empty();
        };
    }

    /**
     * Walk the {@code files} key (if present) and return a list of file descriptors.
     * Each element is a {@code Map<String, Object>} with keys {@code name}, {@code mediaType},
     * {@code src} where present. Unknown keys inside a file entry are preserved verbatim.
     */
    private List<Map<String, Object>> parseFiles(MapPlutusData properties) {
        PlutusData filesProp = properties.getMap().get(BytesPlutusData.of(FILES));
        if (!(filesProp instanceof ListPlutusData filesList)) {
            return null;
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (PlutusData item : filesList.getPlutusDataList()) {
            if (!(item instanceof MapPlutusData fileMap)) {
                continue;
            }
            Map<String, Object> file = new LinkedHashMap<>();
            for (Map.Entry<PlutusData, PlutusData> e : fileMap.getMap().entrySet()) {
                if (!(e.getKey() instanceof BytesPlutusData keyBytes)) {
                    continue;
                }
                String key = bytesToString(keyBytes.getValue());
                Object value = unwrapPlutusValue(e.getValue());
                if (value != null) {
                    file.put(key, value);
                }
            }
            if (!file.isEmpty()) {
                result.add(file);
            }
        }
        return result;
    }

    /**
     * Collect every key in the metadata map that isn't one of the well-known typed scalars
     * or {@code files}. These project-specific properties (attributes, traits, royalties...)
     * go into {@code properties.additional_properties} for downstream consumers.
     */
    private Map<String, Object> parseAdditionalProperties(MapPlutusData properties) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<PlutusData, PlutusData> e : properties.getMap().entrySet()) {
            if (!(e.getKey() instanceof BytesPlutusData keyBytes)) {
                continue;
            }
            String key = bytesToString(keyBytes.getValue());
            if (TYPED_KEYS.contains(key)) {
                continue;
            }
            Object value = unwrapPlutusValue(e.getValue());
            if (value != null) {
                result.put(key, value);
            }
        }
        return result;
    }

    /**
     * Unwrap a Plutus value into a Java type suitable for JSONB serialization. Recurses
     * into maps and lists. Bytes become strings (UTF-8 with null bytes stripped); ints
     * become Long; constructors are flattened to their field list with the alt index.
     */
    private Object unwrapPlutusValue(PlutusData data) {
        return switch (data) {
            case BytesPlutusData b   -> bytesToString(b.getValue());
            case BigIntPlutusData i  -> i.getValue();
            case ListPlutusData list -> {
                List<Object> items = new ArrayList<>();
                for (PlutusData inner : list.getPlutusDataList()) {
                    Object u = unwrapPlutusValue(inner);
                    if (u != null) items.add(u);
                }
                yield items;
            }
            case MapPlutusData map -> {
                Map<String, Object> out = new LinkedHashMap<>();
                for (Map.Entry<PlutusData, PlutusData> entry : map.getMap().entrySet()) {
                    if (!(entry.getKey() instanceof BytesPlutusData kb)) continue;
                    Object u = unwrapPlutusValue(entry.getValue());
                    if (u != null) out.put(bytesToString(kb.getValue()), u);
                }
                yield out;
            }
            case null -> null;
            default -> null;
        };
    }

    private static String bytesToString(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8).replace("\0", "");
    }
}

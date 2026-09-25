package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.parser;

import com.bloxbean.cardano.client.plutus.spec.*;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.ParsedCip68Datum;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.model.Cip68Metadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.util.TokenDecimals;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
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
            return extractDatumProperties(inlineDatum)
                    .map(parts -> buildParsedDatum(parts.properties(), parts.version()));
        } catch (Exception e) {
            log.warn("Unexpected error while parsing CIP-68 datum: {}", inlineDatum, e);
            return Optional.empty();
        }
    }

    /**
     * Strip the CIP-68 datum envelope: a Constr containing a {@code (Map, BigInt)} pair
     * (the metadata properties map and the version integer). Returns empty for any datum
     * that doesn't fit this shape.
     */
    private Optional<DatumParts> extractDatumProperties(String inlineDatum) throws com.bloxbean.cardano.client.exception.CborDeserializationException {
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

        // version is required and stored as a long: reject rather than let longValue() wrap it
        if (version.getValue().bitLength() >= Long.SIZE) {
            log.warn("Ignoring CIP-68 datum with out-of-range version {}", version.getValue());
            return Optional.empty();
        }

        return Optional.of(new DatumParts(properties, version));
    }

    /** Build the typed {@link ParsedCip68Datum} from the unwrapped (Map, version) pair. */
    private ParsedCip68Datum buildParsedDatum(MapPlutusData properties, BigIntPlutusData version) {
        return new ParsedCip68Datum(
                getDecimalsProperty(properties).orElse(null),
                getStringProperty(DESCRIPTION, properties).orElse(null),
                getStringProperty(LOGO, properties).orElse(null),
                getBoundedStringProperty(NAME, properties, Cip68Metadata.NAME_MAX_LENGTH).orElse(null),
                getBoundedStringProperty(TICKER, properties, Cip68Metadata.TICKER_MAX_LENGTH).orElse(null),
                getBoundedStringProperty(URL, properties, Cip68Metadata.URL_MAX_LENGTH).orElse(null),
                version.getValue().longValue(),
                getStringOrChunkedProperty(IMAGE, properties).orElse(null),
                getBoundedStringProperty(MEDIA_TYPE, properties, Cip68Metadata.MEDIA_TYPE_MAX_LENGTH).orElse(null),
                buildPropertiesJson(properties));
    }

    /**
     * Combine {@code files[]} and any non-well-known keys into the single JSONB-backed
     * {@code properties} column. Returns {@code null} if neither part is populated, so
     * pure FT rows don't materialise an empty wrapper.
     */
    private Map<String, Object> buildPropertiesJson(MapPlutusData properties) {
        List<Map<String, Object>> files = parseFiles(properties);
        Map<String, Object> additional = parseAdditionalProperties(properties);

        boolean hasFiles = files != null && !files.isEmpty();
        boolean hasAdditional = !additional.isEmpty();
        if (!hasFiles && !hasAdditional) {
            return null;
        }

        Map<String, Object> json = new LinkedHashMap<>();
        if (hasFiles) {
            json.put(FILES, files);
        }
        if (hasAdditional) {
            json.put("additional_properties", additional);
        }
        return json;
    }

    /** Internal record for the unwrapped CIP-68 envelope ((properties Map, version BigInt)). */
    private record DatumParts(MapPlutusData properties, BigIntPlutusData version) {}

    private Optional<String> getStringProperty(String propertyName, MapPlutusData mapPlutusData) {
        PlutusData property = mapPlutusData.getMap().get(BytesPlutusData.of(propertyName));
        return switch (property) {
            case BytesPlutusData bytes -> Optional.of(bytesToString(bytes.getValue()));
            case null, default -> Optional.empty();
        };
    }

    /**
     * Reads a string property bound for a fixed-width column. Longer values are dropped, since an
     * oversized value would fail the insert and stop the sync; for the required {@code name} that
     * means the datum is then skipped by {@code Cip68TokenService.isValidMetadata}.
     * Length is counted in code points, matching how VARCHAR(n) counts characters.
     */
    private Optional<String> getBoundedStringProperty(String propertyName, MapPlutusData mapPlutusData, int maxLength) {
        return getStringProperty(propertyName, mapPlutusData).filter(value -> {
            int length = value.codePointCount(0, value.length());
            if (length > maxLength) {
                log.warn("Ignoring CIP-68 '{}' of {} characters (max {})", propertyName, length, maxLength);
                return false;
            }
            return true;
        });
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
                yield sb.isEmpty() ? Optional.empty() : Optional.of(sb.toString());
            }
            case null, default -> Optional.empty();
        };
    }

    /**
     * Reads {@code decimals} from the datum. The value is an unbounded on-chain integer, so values that
     * don't fit in a {@code long} are rejected before narrowing: {@code longValue()} alone would
     * silently wrap values above {@code 2^63} into a plausible-looking number. An out-of-range value
     * is dropped (the rest of the metadata is kept) rather than stored.
     */
    private Optional<Long> getDecimalsProperty(MapPlutusData mapPlutusData) {
        PlutusData property = mapPlutusData.getMap().get(BytesPlutusData.of(DECIMALS));
        if (!(property instanceof BigIntPlutusData bigInt)) {
            return Optional.empty();
        }

        BigInteger value = bigInt.getValue();
        // bitLength guard first: longValue() is only exact when the value fits in a long
        if (value.bitLength() >= Long.SIZE || !TokenDecimals.isInRange(value.longValue())) {
            log.warn("Ignoring out-of-range CIP-68 decimals {} (allowed {})", value, TokenDecimals.RANGE);
            return Optional.empty();
        }

        return Optional.of(value.longValue());
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
        properties.getMap().entrySet().stream()
                .filter(e -> e.getKey() instanceof BytesPlutusData)
                .map(e -> Map.entry(
                        bytesToString(((BytesPlutusData) e.getKey()).getValue()),
                        unwrapPlutusValue(e.getValue())))
                .filter(e -> !TYPED_KEYS.contains(e.getKey()) && e.getValue() != null)
                .forEach(e -> result.put(e.getKey(), e.getValue()));
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

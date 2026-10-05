package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.parser;

import com.bloxbean.cardano.client.plutus.spec.*;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.store.common.util.StringUtil;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.AssetType;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.Cip68Constants;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.ParsedCip68Datum;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.model.Cip68Metadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.util.TokenDecimals;
import jakarta.annotation.Nullable;
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

    /** CIP-68 version 4 wraps the metadata in a CIP-25 style map: {"721": {policy_id: {asset_name: metadata}}}. */
    private static final BytesPlutusData NESTED_MAP_KEY = BytesPlutusData.of("721");
    private static final long NESTED_MAP_MIN_VERSION = 4;

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
        return parse(inlineDatum, null);
    }

    /**
     * Same as {@link #parse(String)}, but resolves a version 4 nested-map datum to the entry for
     * {@code referenceNft}. Without it, a nested map is only resolved when it has a single entry.
     */
    public Optional<ParsedCip68Datum> parse(String inlineDatum, @Nullable AssetType referenceNft) {
        if (inlineDatum == null || inlineDatum.isBlank()) {
            return Optional.empty();
        }

        try {
            return extractDatumProperties(inlineDatum)
                    .flatMap(parts -> resolveMetadata(parts, referenceNft)
                            .map(metadata -> buildParsedDatum(metadata, parts.version())));
        } catch (StackOverflowError e) {
            // TODO: temporary workaround. Remove once cardano-client-lib decodes CBOR without
            //  recursion (bloxbean/cardano-client-lib#681).
            // The CBOR decoder recurses once per nesting level, and the ledger bounds a datum only by
            // transaction size, so a valid on-chain datum can be nested deeper than the stack allows.
            // StackOverflowError is an Error, not an Exception, so it needs its own catch: skip the
            // datum like any other undecodable one.
            log.warn("Skipping CIP-68 datum nested too deeply to decode ({} bytes)", inlineDatum.length() / 2);
            return Optional.empty();
        } catch (Exception e) {
            // One line per failure, with the datum for reproduction; the stack trace only at DEBUG,
            // so a run of unparseable datums doesn't flood the sync log.
            log.warn("Skipping unparseable CIP-68 datum ({}): {}", e, inlineDatum);
            log.debug("CIP-68 datum parse failure", e);
            return Optional.empty();
        } catch (StackOverflowError e) {
            //Deeply nested datum, CCL PlutusData deserialization is recursive
            log.warn("Unable to parse deeply nested CIP-68 datum. Datum cbor length: {}", inlineDatum.length());
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

        // version is required and stored as a long, but a datum integer is unbounded: reject values
        // that don't fit rather than let longValue() silently wrap them (2^64 + 1 would become 1)
        BigInteger versionValue = version.getValue();
        if (versionValue.bitLength() >= Long.SIZE) {
            log.warn("Ignoring CIP-68 datum with out-of-range version {}", versionValue);
            return Optional.empty();
        }

        return Optional.of(new DatumParts(properties, versionValue.longValue()));
    }

    /**
     * Returns the metadata map to read fields from. Versions 1–3 carry it directly; version 4 nests it
     * under {@code "721" -> policy_id -> asset_name} (asset name without the label prefix, both as raw
     * bytes). A version 4 datum without the {@code "721"} key is read directly, as before.
     */
    private Optional<MapPlutusData> resolveMetadata(DatumParts parts, @Nullable AssetType referenceNft) {
        MapPlutusData properties = parts.properties();
        if (parts.version() < NESTED_MAP_MIN_VERSION
                || !(properties.getMap().get(NESTED_MAP_KEY) instanceof MapPlutusData byPolicy)) {
            return Optional.of(properties);
        }

        if (referenceNft == null) {
            // No asset context: only an unambiguous single entry can be resolved
            return singleValue(byPolicy)
                    .flatMap(Cip68DatumParser::singleValue);
        }

        String assetNameWithoutLabel = referenceNft.assetName().substring(Cip68Constants.REFERENCE_TOKEN_PREFIX.length());
        return asMap(byPolicy.getMap().get(BytesPlutusData.of(HexUtil.decodeHexString(referenceNft.policyId()))))
                .flatMap(byAsset -> asMap(byAsset.getMap().get(BytesPlutusData.of(HexUtil.decodeHexString(assetNameWithoutLabel)))));
    }

    private static Optional<MapPlutusData> singleValue(MapPlutusData map) {
        return map.getMap().size() == 1 ? asMap(map.getMap().values().iterator().next()) : Optional.empty();
    }

    private static Optional<MapPlutusData> asMap(@Nullable PlutusData data) {
        return data instanceof MapPlutusData map ? Optional.of(map) : Optional.empty();
    }

    /** Build the typed {@link ParsedCip68Datum} from the unwrapped (Map, version) pair. */
    private ParsedCip68Datum buildParsedDatum(MapPlutusData properties, long version) {
        return new ParsedCip68Datum(
                getDecimalsProperty(properties).orElse(null),
                getStringProperty(DESCRIPTION, properties).orElse(null),
                getStringOrChunkedProperty(LOGO, properties).orElse(null),
                getBoundedStringProperty(NAME, properties, Cip68Metadata.NAME_MAX_LENGTH).orElse(null),
                getBoundedStringProperty(TICKER, properties, Cip68Metadata.TICKER_MAX_LENGTH).orElse(null),
                getBoundedStringProperty(URL, properties, Cip68Metadata.URL_MAX_LENGTH).orElse(null),
                version,
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

    /** Internal record for the unwrapped CIP-68 envelope (properties Map, range-checked version). */
    private record DatumParts(MapPlutusData properties, long version) {}

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
     * <p>
     * Length is counted in UTF-16 units ({@link String#length()}), not code points: H2 counts
     * {@code VARCHAR(n)} that way, so 255 emoji (510 units) overflow a {@code VARCHAR(255)} there.
     * Postgres and MySQL count code points, which is never more than UTF-16 units, so this bound
     * is safe on every supported database, at the cost of rejecting some long non-BMP names that
     * Postgres alone could have stored.
     */
    private Optional<String> getBoundedStringProperty(String propertyName, MapPlutusData mapPlutusData, int maxLength) {
        return getStringProperty(propertyName, mapPlutusData).filter(value -> {
            int length = value.length();
            if (length > maxLength) {
                log.warn("Ignoring CIP-68 '{}' of {} characters (max {})", propertyName, length, maxLength);
                return false;
            }
            return true;
        });
    }

    /**
     * CIP-25 convention, inherited by CIP-68 NFT {@code image} and the FT {@code logo} (both a
     * CIP-68 {@code uri = bounded_bytes / [* bounded_bytes]}): if a string value exceeds 64 bytes
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
                String key = bytesToText(keyBytes.getValue());
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
            String key = bytesToText(keyBytes.getValue());
            // Check before storing: an unsupported value must drop only this property, not the datum
            Object value = TYPED_KEYS.contains(key) ? null : unwrapPlutusValue(e.getValue());
            if (value != null) {
                result.put(key, value);
            }
        }
        return result;
    }

    /**
     * Unwrap a Plutus value into a Java type suitable for JSONB serialization. Recurses into maps,
     * lists and constructors. Bytes become text via {@link #bytesToText}; ints become
     * {@link BigInteger}; a constructor becomes {@code {"constructor": <alternative>, "fields": [...]}},
     * the field names cardano-cli uses for Plutus data in its detailed JSON schema.
     */
    private Object unwrapPlutusValue(PlutusData data) {
        return switch (data) {
            case BytesPlutusData b   -> bytesToText(b.getValue());
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
                    if (u != null) out.put(bytesToText(kb.getValue()), u);
                }
                yield out;
            }
            case ConstrPlutusData constr -> {
                Object fields = unwrapPlutusValue(constr.getData());
                Map<String, Object> out = new LinkedHashMap<>();
                out.put("constructor", constr.getAlternative());
                out.put("fields", fields != null ? fields : List.of());
                yield out;
            }
            case null -> null;
            default -> null;
        };
    }

    private static String bytesToString(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8).replace("\0", "");
    }

    /**
     * Bytes as text the way CIP-68 says to convert metadata to JSON: UTF-8 when the bytes are valid
     * UTF-8, hex otherwise (hashes, key hashes and other binary values). Null characters are
     * stripped from text, as in {@link #bytesToString}.
     */
    private static String bytesToText(byte[] bytes) {
        return StringUtil.isValidUTF8(bytes) ? bytesToString(bytes) : HexUtil.encodeHexString(bytes);
    }
}

package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.parser;

import com.bloxbean.cardano.client.plutus.spec.*;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.store.common.util.StringUtil;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.metrics.Cip68Metrics;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.AssetType;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.Cip68Constants;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.Cip68Uri;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.ParsedCip68Datum;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.model.Cip68Metadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.util.TokenDecimals;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
@Slf4j
public class Cip68DatumParser {

    private final Cip68Metrics metrics;

    @Autowired
    public Cip68DatumParser(Cip68Metrics metrics) {
        this.metrics = metrics;
    }

    /** Without metrics that anyone reads (tests). */
    public Cip68DatumParser() {
        this(Cip68Metrics.noop());
    }

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

    /** Largest {@code logo} or {@code image} accepted, in bytes. Same limit as the CIP-26 logo. */
    public static final int URI_MAX_BYTES = 64 * 1024;

    /**
     * Deepest nesting allowed in the value of an additional property (a list inside a list counts as two levels). The
     * value goes to a JSON column, and Jackson refuses to write or read a document nested deeper than 1000 levels, so
     * a deeper value would fail the insert and stop the sync. Real metadata nests a few levels; the datum is untrusted
     * and only the transaction size bounds how deep it can be.
     */
    public static final int MAX_PROPERTY_DEPTH = 100;

    /** CIP-68 version 4 wraps the metadata in a CIP-25 style map: {"721": {policy_id: {asset_name: metadata}}}. */
    private static final BytesPlutusData NESTED_MAP_KEY = BytesPlutusData.of("721");
    /**
     * The datum versions CIP-68 defines. They are informational: how a datum is read does not depend on its
     * version but on its structure (see {@link #isNested}), as the CIP's retrieval steps say. A datum with another
     * version is still read and indexed, and a warning is logged so a new version is noticed. Update the upper bound
     * when the CIP adds a version.
     */
    static final long MIN_DEFINED_VERSION = 1;
    static final long MAX_DEFINED_VERSION = 4;

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
                    .filter(parts -> isDefinedVersion(parts, referenceNft))
                    .flatMap(parts -> resolveMetadata(parts, referenceNft)
                            .map(metadata -> buildParsedDatum(metadata, parts.version(), referenceNft)));
        } catch (StackOverflowError e) {
            // TODO: temporary workaround. Remove once cardano-client-lib decodes CBOR without
            //  recursion (bloxbean/cardano-client-lib#681).
            // The CBOR decoder recurses once per nesting level, and the ledger bounds a datum only by
            // transaction size, so a valid on-chain datum can be nested deeper than the stack allows.
            // StackOverflowError is an Error, not an Exception, so it needs its own catch: skip the
            // datum like any other undecodable one.
            log.warn("Skipping CIP-68 datum nested too deeply to decode ({} bytes)", inlineDatum.length() / 2);
            metrics.parseFailure();
            return Optional.empty();
        } catch (Exception e) {
            // One line per failure, with the datum for reproduction; the stack trace only at DEBUG,
            // so a run of unparseable datums doesn't flood the sync log.
            log.warn("Skipping unparseable CIP-68 datum ({}): {}", e, inlineDatum);
            log.debug("CIP-68 datum parse failure", e);
            metrics.parseFailure();
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
        if (!isNested(parts)) {
            return Optional.of(properties);
        }
        MapPlutusData byPolicy = (MapPlutusData) properties.getMap().get(NESTED_MAP_KEY);

        if (referenceNft == null) {
            // No asset context: only an unambiguous single entry can be resolved
            return singleValue(byPolicy)
                    .flatMap(Cip68DatumParser::singleValue);
        }

        String assetNameWithoutLabel = referenceNft.assetName().substring(Cip68Constants.REFERENCE_TOKEN_PREFIX.length());
        return asMap(byPolicy.getMap().get(BytesPlutusData.of(HexUtil.decodeHexString(referenceNft.policyId()))))
                .flatMap(byAsset -> asMap(byAsset.getMap().get(BytesPlutusData.of(HexUtil.decodeHexString(assetNameWithoutLabel)))));
    }

    /**
     * CIP-68 defines versions 1 to 4 (its CDDL lists them, and says a change that is not backwards-compatible adds a
     * new version). A datum with another version is not indexed: the layout of a version the CIP does not define is
     * a guess, and a new version is added here when the CIP adds it. One warning names the token and the version, and
     * the datum is counted as skipped with the reason {@code invalid_version}. The layout of versions 1 to 4 still
     * comes from the structure (the {@code "721"} key), not from the version.
     *
     * @return true if the version is one CIP-68 defines
     */
    private boolean isDefinedVersion(DatumParts parts, @Nullable AssetType referenceNft) {
        long version = parts.version();
        if (version >= MIN_DEFINED_VERSION && version <= MAX_DEFINED_VERSION) {
            return true;
        }
        if (referenceNft != null) {
            log.warn("Skipping CIP-68 datum of {}/{}: version {} is not one CIP-68 defines ({} to {})",
                    referenceNft.policyId(), referenceNft.assetName(), version, MIN_DEFINED_VERSION, MAX_DEFINED_VERSION);
        } else {
            log.warn("Skipping CIP-68 datum: version {} is not one CIP-68 defines ({} to {})",
                    version, MIN_DEFINED_VERSION, MAX_DEFINED_VERSION);
        }
        metrics.invalidVersion();
        return false;
    }

    /**
     * Whether the metadata map is the nested format: it has the {@code "721"} key, whose value is a map. This is the
     * test in step 4 of the CIP's retrieval steps ("direct metadata (map without "721" key) or nested map format
     * (map with "721" key)"), and it does not depend on the version. A flat map with an additional property named
     * {@code "721"} that holds a map would be misread; none exists on mainnet.
     */
    private static boolean isNested(DatumParts parts) {
        return parts.properties().getMap().get(NESTED_MAP_KEY) instanceof MapPlutusData;
    }

    /**
     * Whether the datum is in the nested format, which can carry the metadata of several reference NFTs.
     * A flat datum (a map without {@code "721"}) describes one token. Anything that is
     * not a CIP-68 datum, or cannot be decoded, is not nested.
     */
    public boolean hasNestedMetadata(@Nullable String inlineDatum) {
        if (inlineDatum == null || inlineDatum.isBlank()) {
            return false;
        }
        try {
            return extractDatumProperties(inlineDatum).map(Cip68DatumParser::isNested).orElse(false);
        } catch (Exception | StackOverflowError e) {
            return false;
        }
    }

    private static Optional<MapPlutusData> singleValue(MapPlutusData map) {
        return map.getMap().size() == 1 ? asMap(map.getMap().values().iterator().next()) : Optional.empty();
    }

    private static Optional<MapPlutusData> asMap(@Nullable PlutusData data) {
        return data instanceof MapPlutusData map ? Optional.of(map) : Optional.empty();
    }

    /** Build the typed {@link ParsedCip68Datum} from the unwrapped (Map, version) pair. */
    private ParsedCip68Datum buildParsedDatum(MapPlutusData properties, long version, @Nullable AssetType referenceNft) {
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
                buildPropertiesJson(properties, referenceNft));
    }

    /**
     * Combine {@code files[]} and any non-well-known keys into the single JSONB-backed
     * {@code properties} column. Returns {@code null} if neither part is populated, so
     * pure FT rows don't materialise an empty wrapper.
     */
    private Map<String, Object> buildPropertiesJson(MapPlutusData properties, @Nullable AssetType referenceNft) {
        List<Map<String, Object>> files = parseFiles(properties, referenceNft);
        Map<String, Object> additional = parseAdditionalProperties(properties, referenceNft);

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

    /**
     * Reads a text property. CIP-68 says text is UTF-8, so valid UTF-8 is stored as text. Bytes that are
     * not valid UTF-8 are stored as hex, the same rule as for additional properties, instead of being
     * decoded with replacement characters, which would lose the original bytes.
     */
    private Optional<String> getStringProperty(String propertyName, MapPlutusData mapPlutusData) {
        PlutusData property = mapPlutusData.getMap().get(BytesPlutusData.of(propertyName));
        return switch (property) {
            case BytesPlutusData bytes -> Optional.of(bytesToText(bytes.getValue()));
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
     * Reads a CIP-68 {@code uri} ({@code uri = bounded_bytes / [* bounded_bytes]}), used for the NFT
     * {@code image} and the FT {@code logo}: a value longer than 64 bytes, the most a Plutus byte string
     * holds, is split into a list of byte-string chunks, and this joins them back together. The chunks are
     * joined as bytes and decoded once, so a multi-byte character cut by a chunk boundary survives.
     * Elements of the list that are not byte strings are ignored.
     * <p>
     * The value is capped at {@value #URI_MAX_BYTES} bytes (the CIP-26 logo has the same limit): an
     * over-long value is dropped with a warning and the rest of the datum is kept. The scheme is not
     * checked here; {@code Cip68TokenService#invalidReason} checks the image of a 222 or 444 token and the logo of a 333 token.
     */
    private Optional<String> getStringOrChunkedProperty(String propertyName, MapPlutusData mapPlutusData) {
        PlutusData property = mapPlutusData.getMap().get(BytesPlutusData.of(propertyName));

        byte[] value = switch (property) {
            case BytesPlutusData bytes -> bytes.getValue();
            case ListPlutusData list -> joinChunks(list);
            case null, default -> null;
        };
        if (value == null) {
            return Optional.empty();
        }
        if (value.length > URI_MAX_BYTES) {
            log.warn("Ignoring CIP-68 '{}' of {} bytes (max {})", propertyName, value.length, URI_MAX_BYTES);
            return Optional.empty();
        }
        // a single byte string keeps an empty value ("" is what some NFTs declare); an empty list is nothing
        return property instanceof ListPlutusData && value.length == 0 ? Optional.empty() : Optional.of(bytesToText(value));
    }

    private static byte[] joinChunks(ListPlutusData list) {
        java.io.ByteArrayOutputStream joined = new java.io.ByteArrayOutputStream();
        for (PlutusData chunk : list.getPlutusDataList()) {
            if (chunk instanceof BytesPlutusData b) {
                joined.writeBytes(b.getValue());
            }
        }
        return joined.toByteArray();
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
     * <p>
     * {@code files} is optional, so one that breaks the CIP-68 definition is left out as a whole, with a warning, and
     * the rest of the datum is kept: every entry has to be a map with a {@code mediaType} byte string and a
     * {@code src} that is a URI with one of the allowed schemes.
     */
    private List<Map<String, Object>> parseFiles(MapPlutusData properties, @Nullable AssetType referenceNft) {
        PlutusData filesProp = properties.getMap().get(BytesPlutusData.of(FILES));
        if (!(filesProp instanceof ListPlutusData filesList)) {
            return null;
        }
        for (PlutusData item : filesList.getPlutusDataList()) {
            Optional<String> invalid = invalidFileReason(item);
            if (invalid.isPresent()) {
                warnFilesDropped(invalid.get(), referenceNft);
                return null;
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (PlutusData item : filesList.getPlutusDataList()) {
            if (item instanceof MapPlutusData fileMap) {
                Map<String, Object> file = readEntries(fileMap, "files[]", false, referenceNft);
                if (!file.isEmpty()) {
                    result.add(file);
                }
            }
        }
        return result;
    }

    /** Why an entry of {@code files} breaks {@code files_details} of the CIP-68 definition, or empty if it does not. */
    private Optional<String> invalidFileReason(PlutusData item) {
        if (!(item instanceof MapPlutusData file)) {
            return Optional.of("an entry of 'files' is not a map");
        }
        if (!(file.getMap().get(BytesPlutusData.of(MEDIA_TYPE)) instanceof BytesPlutusData)) {
            return Optional.of("an entry of 'files' has no mediaType");
        }
        Optional<String> src = getStringOrChunkedProperty("src", file);
        if (src.isEmpty() || src.get().isBlank()) {
            return Optional.of("an entry of 'files' has no usable src");
        }
        if (!Cip68Uri.hasAllowedScheme(src.get())) {
            String shown = src.get().length() <= 60 ? src.get() : src.get().substring(0, 60) + "...";
            return Optional.of("the src '" + shown + "' of an entry of 'files' is not a URI with one of the schemes CIP-68 allows ("
                    + Cip68Uri.ALLOWED_SCHEMES + ")");
        }
        return Optional.empty();
    }

    private void warnFilesDropped(String reason, @Nullable AssetType referenceNft) {
        metrics.propertyDropped(Cip68Metrics.INVALID_FILES);
        if (referenceNft != null) {
            log.warn("CIP-68 datum of {}/{}: dropping property 'files' and keeping the rest, because {}",
                    referenceNft.policyId(), referenceNft.assetName(), reason);
        } else {
            log.warn("CIP-68 datum: dropping property 'files' and keeping the rest, because {}", reason);
        }
    }

    /**
     * Collect every key in the metadata map that isn't one of the well-known typed scalars
     * or {@code files}. These project-specific properties (attributes, traits, royalties...)
     * go into {@code properties.additional_properties} for downstream consumers.
     */
    private Map<String, Object> parseAdditionalProperties(MapPlutusData properties, @Nullable AssetType referenceNft) {
        return readEntries(properties, "additional property", true, referenceNft);
    }

    /**
     * Converts the entries of a metadata map to JSON-ready values, keyed by the text of the key. {@code where} names
     * the map in warnings ({@code files[]} entries are reported as {@code files[].<key>}); {@code skipTypedKeys} leaves
     * out the keys that have a typed column.
     */
    private Map<String, Object> readEntries(MapPlutusData map, String where, boolean skipTypedKeys, @Nullable AssetType referenceNft) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<PlutusData, PlutusData> e : byteStringKeysFirst(map)) {
            readEntry(result, e, where, skipTypedKeys, referenceNft);
        }
        return result;
    }

    private void readEntry(Map<String, Object> result, Map.Entry<PlutusData, PlutusData> entry, String where,
                           boolean skipTypedKeys, @Nullable AssetType referenceNft) {
        String key = keyText(entry.getKey());
        if (key == null) {
            warnKeyLeftOut(where, entry.getKey(), referenceNft);
            return;
        }
        String reportedKey = skipTypedKeys ? key : where + "." + key;
        if (skipTypedKeys && TYPED_KEYS.contains(key)) {
            return;
        }
        // the depth check comes first: it is bounded, while the constructor check recurses as deep as the value goes
        if (dropsTooDeepProperty(reportedKey, entry.getValue(), referenceNft) || dropsConstructorProperty(reportedKey, entry.getValue(), referenceNft)) {
            return;
        }
        // Check before storing: an unsupported value must drop only this property, not the datum
        Object value = unwrapPlutusValue(entry.getValue());
        if (value != null) {
            putFirst(result, key, value, referenceNft);
        }
    }

    /**
     * The text of a map key. The CIP-68 definition allows any metadata as a key ({@code { * metadata => metadata }}),
     * and JSON needs text keys, so a byte string key is read as text or hex (like any byte string) and an integer key
     * as its decimal string, which is what Lucid and Blockfrost do. A list, map or constructor key has no sensible text
     * form: {@code null}, and the entry is left out.
     */
    private static String keyText(PlutusData key) {
        return switch (key) {
            case BytesPlutusData bytes -> bytesToText(bytes.getValue());
            case BigIntPlutusData integer -> integer.getValue().toString();
            case null, default -> null;
        };
    }

    private void warnKeyLeftOut(String where, PlutusData key, @Nullable AssetType referenceNft) {
        metrics.propertyDropped("key_unsupported");
        String kind = switch (key) {
            case ListPlutusData ignored -> "list";
            case MapPlutusData ignored -> "map";
            case ConstrPlutusData ignored -> "constructor";
            case null, default -> "unsupported";
        };
        if (referenceNft != null) {
            log.warn("CIP-68 datum of {}/{}: leaving out a {} entry whose key is a {}, which has no text form",
                    referenceNft.policyId(), referenceNft.assetName(), where, kind);
        } else {
            log.warn("CIP-68 datum: leaving out a {} entry whose key is a {}, which has no text form", where, kind);
        }
    }

    /**
     * The entries of a map with the byte string keys first, so that when a byte string key and an integer key read the
     * same ({@code "1"} and {@code 1}) the byte string key is the one that stays, whatever order the map iterates in.
     */
    private static List<Map.Entry<PlutusData, PlutusData>> byteStringKeysFirst(MapPlutusData map) {
        return map.getMap().entrySet().stream()
                .sorted(java.util.Comparator.comparingInt(e -> e.getKey() instanceof BytesPlutusData ? 0 : 1))
                .toList();
    }

    /** Two keys that read the same (the byte string "1" and the integer 1): the byte string key stays. */
    private void putFirst(Map<String, Object> target, String key, Object value, @Nullable AssetType referenceNft) {
        if (target.putIfAbsent(key, value) != null) {
            metrics.propertyDropped("key_collision");
            if (referenceNft != null) {
                log.warn("CIP-68 datum of {}/{}: two keys read as '{}', keeping the byte string key",
                        referenceNft.policyId(), referenceNft.assetName(), key.length() <= 60 ? key : key.substring(0, 60) + "...");
            } else {
                log.warn("CIP-68 datum: two keys read as '{}', keeping the byte string key", key.length() <= 60 ? key : key.substring(0, 60) + "...");
            }
        }
    }

    /**
     * The generic CIP-68 definition allows a metadata value to be a map, a list, an integer or a byte string, and
     * Plutus data of any kind only in {@code extra}. A property whose value holds a constructor, however deep, is
     * therefore not valid metadata: it is dropped with a warning, and the rest of the datum, and the token, are
     * kept. (Before, the constructor was stored as {@code {"constructor": n, "fields": [...]}}, a form the CIP does
     * not define.)
     *
     * @return true if the property was dropped
     */
    private boolean dropsConstructorProperty(String key, PlutusData value, @Nullable AssetType referenceNft) {
        if (!containsConstructor(value)) {
            return false;
        }
        metrics.propertyDropped("constructor");
        String shortKey = key.length() <= 60 ? key : key.substring(0, 60) + "...";
        if (referenceNft != null) {
            log.warn("CIP-68 datum of {}/{}: dropping property '{}' and keeping the rest, because its value holds a Plutus "
                            + "constructor, which CIP-68 metadata does not allow",
                    referenceNft.policyId(), referenceNft.assetName(), shortKey);
        } else {
            log.warn("CIP-68 datum: dropping property '{}' and keeping the rest, because its value holds a Plutus "
                    + "constructor, which CIP-68 metadata does not allow", shortKey);
        }
        return true;
    }

    /**
     * Drops a property whose value is nested deeper than {@link #MAX_PROPERTY_DEPTH}, with a warning and a count; the
     * rest of the datum is kept.
     */
    private boolean dropsTooDeepProperty(String key, PlutusData value, @Nullable AssetType referenceNft) {
        if (!isNestedDeeperThan(value, MAX_PROPERTY_DEPTH)) {
            return false;
        }
        metrics.propertyDropped(Cip68Metrics.TOO_DEEP);
        String shortKey = key.length() <= 60 ? key : key.substring(0, 60) + "...";
        if (referenceNft != null) {
            log.warn("CIP-68 datum of {}/{}: dropping property '{}' and keeping the rest, because its value is nested "
                            + "deeper than {} levels",
                    referenceNft.policyId(), referenceNft.assetName(), shortKey, MAX_PROPERTY_DEPTH);
        } else {
            log.warn("CIP-68 datum: dropping property '{}' and keeping the rest, because its value is nested deeper "
                    + "than {} levels", shortKey, MAX_PROPERTY_DEPTH);
        }
        return true;
    }

    /** True if the value has more than {@code levels} levels of lists, maps and constructors. Stops at {@code levels}, so it never recurses deeper than that. */
    private static boolean isNestedDeeperThan(@Nullable PlutusData data, int levels) {
        List<PlutusData> children = switch (data) {
            case ListPlutusData list -> list.getPlutusDataList();
            case MapPlutusData map -> {
                List<PlutusData> all = new ArrayList<>();
                map.getMap().forEach((k, v) -> {
                    all.add(k);
                    all.add(v);
                });
                yield all;
            }
            case ConstrPlutusData constr -> constr.getData().getPlutusDataList();
            case null, default -> null;
        };
        if (children == null) {
            return false;
        }
        return levels == 0 || children.stream().anyMatch(child -> isNestedDeeperThan(child, levels - 1));
    }

    private static boolean containsConstructor(@Nullable PlutusData data) {
        return switch (data) {
            case ConstrPlutusData ignored -> true;
            case ListPlutusData list -> list.getPlutusDataList().stream().anyMatch(Cip68DatumParser::containsConstructor);
            case MapPlutusData map -> map.getMap().entrySet().stream()
                    .anyMatch(e -> containsConstructor(e.getKey()) || containsConstructor(e.getValue()));
            case null, default -> false;
        };
    }

    /**
     * Unwrap a Plutus value into a Java type suitable for JSONB serialization. Recurses into maps
     * and lists. Bytes become text via {@link #bytesToText}; ints become {@link BigInteger}. A constructor
     * has no place in CIP-68 metadata, so it never gets here: {@link #dropsConstructorProperty} drops the
     * property that holds one before the value is converted.
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
                for (Map.Entry<PlutusData, PlutusData> entry : byteStringKeysFirst(map)) {
                    String key = keyText(entry.getKey());
                    if (key == null) continue;
                    Object u = unwrapPlutusValue(entry.getValue());
                    if (u != null) out.putIfAbsent(key, u);
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

    /**
     * Bytes as text the way CIP-68 says to convert metadata to JSON: UTF-8 when the bytes are valid
     * UTF-8, hex otherwise (hashes, key hashes and other binary values). Null characters are
     * stripped from text, as in {@link #bytesToString}.
     */
    private static String bytesToText(byte[] bytes) {
        return StringUtil.isValidUTF8(bytes) ? bytesToString(bytes) : HexUtil.encodeHexString(bytes);
    }
}

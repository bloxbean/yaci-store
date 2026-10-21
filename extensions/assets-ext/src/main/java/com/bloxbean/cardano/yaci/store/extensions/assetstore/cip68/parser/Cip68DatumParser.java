package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.parser;

import com.bloxbean.cardano.client.plutus.spec.*;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.store.common.util.StringUtil;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.metrics.Cip68Metrics;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.AssetType;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.Cip68Constants;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.Cip68Uri;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.ParseOutcome;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.ParsedCip68Datum;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.invalidtoken.InvalidTokenReason;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.util.TokenDecimals;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.validation.ValidationCode;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.validation.ValidationError;
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

import static com.bloxbean.cardano.yaci.store.extensions.assetstore.validation.ValidationError.CIP_68;

/**
 * Reads a CIP-68 reference NFT datum.
 * <p>
 * The parser indexes what is on-chain and reports what departs from CIP-68: a value that breaks the CIP is kept as it
 * was written, and a {@link ValidationError} says what is wrong with it. Whether the token has what its label requires
 * is checked per label by {@link com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.service.Cip68Validator}.
 * A datum is left out only when it cannot be decoded, or when it is an obvious attack on the indexer: one nested so deep
 * that it cannot be decoded, or a property nested more than {@value #MAX_PROPERTY_DEPTH} levels (see ADR 0006).
 */
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

    /** The codes of the validation errors the parser reports (the validator has its own for the fields a label requires). */
    public static final String WRONG_TYPE = ValidationCode.WRONG_TYPE;
    public static final String NOT_UTF8 = "not_utf8";
    public static final String OUT_OF_RANGE = ValidationCode.OUT_OF_RANGE;
    public static final String UNDEFINED_VERSION = ValidationCode.UNDEFINED_VERSION;
    public static final String PLUTUS_CONSTRUCTOR = ValidationCode.PLUTUS_CONSTRUCTOR;
    public static final String UNSUPPORTED_KEY = ValidationCode.UNSUPPORTED_KEY;
    public static final String KEY_COLLISION = ValidationCode.KEY_COLLISION;
    public static final String TOO_DEEP = ValidationCode.TOO_DEEP;
    public static final String INVALID_FILES_ENTRY = ValidationCode.INVALID_FILES_ENTRY;
    public static final String NO_NESTED_ENTRY = ValidationCode.NO_NESTED_ENTRY;

    /**
     * Deepest nesting allowed in the value of an additional property (a list inside a list counts as two levels). The
     * value goes to a JSON column, and Jackson refuses to write or read a document nested deeper than 1000 levels, so
     * a deeper value would fail the insert and stop the sync. Real metadata nests a few levels; the datum is untrusted
     * and only the transaction size bounds how deep it can be. A deeper property is an obvious attack and is dropped,
     * with a warning (https://github.com/bloxbean/yaci-store/issues/1240).
     */
    public static final int MAX_PROPERTY_DEPTH = 100;

    /** CIP-68 version 4 wraps the metadata in a CIP-25 style map: {"721": {policy_id: {asset_name: metadata}}}. */
    private static final BytesPlutusData NESTED_MAP_KEY = BytesPlutusData.of("721");
    /**
     * The datum versions CIP-68 defines. A datum with another version is still indexed, with an error on the
     * {@code version}. How a datum is read does not depend on the version but on its structure (see {@link #isNested}),
     * as the CIP's retrieval steps say. Update the upper bound when the CIP adds a version.
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
     * <p>
     * The result is empty for a datum that is not a CIP-68 datum or cannot be decoded.
     */
    public Optional<ParsedCip68Datum> parse(String inlineDatum, @Nullable AssetType referenceNft) {
        return parseDetailed(inlineDatum, referenceNft) instanceof ParseOutcome.Parsed(ParsedCip68Datum datum)
                ? Optional.of(datum)
                : Optional.empty();
    }

    /**
     * Same as {@link #parse(String, AssetType)}, but says why there is no result: the datum is not a CIP-68 datum (ignored), or it is dropped
     * because it cannot be decoded or is nested too deeply to be decoded (counted and warned about here, recorded by the caller).
     */
    public ParseOutcome parseDetailed(String inlineDatum, @Nullable AssetType referenceNft) {
        if (inlineDatum == null || inlineDatum.isBlank()) {
            return new ParseOutcome.NotCip68();
        }

        try {
            return extractDatumProperties(inlineDatum)
                    .<ParseOutcome>map(parts -> new ParseOutcome.Parsed(buildParsedDatum(parts, referenceNft)))
                    .orElseGet(ParseOutcome.NotCip68::new);
        } catch (StackOverflowError e) {
            // TODO: temporary workaround. Remove once cardano-client-lib decodes CBOR without
            //  recursion (bloxbean/cardano-client-lib#681).
            // The CBOR decoder recurses once per nesting level, and the ledger bounds a datum only by
            // transaction size, so a valid on-chain datum can be nested deeper than the stack allows.
            // StackOverflowError is an Error, not an Exception, so it needs its own catch. A datum nested this
            // deep is an obvious attack on the indexer: it is left out, like any undecodable one.
            log.warn("Skipping the CIP-68 datum of {}, nested too deeply to decode, this looks like an attack ({} bytes): {}",
                    describe(referenceNft), inlineDatum.length() / 2, abbreviateDatum(inlineDatum));
            metrics.parseFailure();
            return new ParseOutcome.Dropped(InvalidTokenReason.DATUM_TOO_DEEP_TO_DECODE,
                    "the datum is nested too deeply to decode (" + inlineDatum.length() / 2 + " bytes), this looks like an attack");
        } catch (Exception e) {
            // One line per failure, with the datum for reproduction; the stack trace only at DEBUG,
            // so a run of unparseable datums doesn't flood the sync log.
            log.warn("Skipping the unparseable CIP-68 datum of {} ({}): {}", describe(referenceNft), e, inlineDatum);
            log.debug("CIP-68 datum parse failure", e);
            metrics.parseFailure();
            return new ParseOutcome.Dropped(InvalidTokenReason.DATUM_UNDECODABLE, "the datum cannot be decoded: " + e);
        }
    }

    /**
     * Strip the CIP-68 datum envelope: a Constr whose first field is the metadata map, followed by the version integer.
     * A datum with the map but without an integer version is still read (the CIP requires the version, so that is a
     * {@code MANDATORY} error on the row, not a reason to hide the token). Returns empty for any datum that is not a
     * Constr with a map first.
     */
    private Optional<DatumParts> extractDatumProperties(String inlineDatum) throws com.bloxbean.cardano.client.exception.CborDeserializationException {
        PlutusData plutusData = PlutusData.deserialize(HexUtil.decodeHexString(inlineDatum));

        if (!(plutusData instanceof ConstrPlutusData cip68Data)) {
            return Optional.empty();
        }

        List<PlutusData> dataList = cip68Data.getData().getPlutusDataList();
        if (dataList.isEmpty() || !(dataList.getFirst() instanceof MapPlutusData properties)) {
            return Optional.empty();
        }

        if (dataList.size() < 2) {
            return Optional.of(new DatumParts(properties, null, ABSENT));
        }
        if (!(dataList.get(1) instanceof BigIntPlutusData version)) {
            return Optional.of(new DatumParts(properties, null, typeOf(dataList.get(1))));
        }

        return Optional.of(new DatumParts(properties, version.getValue(), null));
    }

    /**
     * Returns the metadata map to read fields from. Versions 1–3 carry it directly; version 4 nests it
     * under {@code "721" -> policy_id -> asset_name} (asset name without the label prefix, both as raw
     * bytes). A version 4 datum without the {@code "721"} key is read directly, as before.
     * <p>
     * A nested datum with no usable entry for the token (another policy, another asset name, or an entry that is not a
     * map) has no metadata to read: the token is still indexed, with an error that says so.
     */
    private Optional<MapPlutusData> resolveMetadata(DatumParts parts, @Nullable AssetType referenceNft) {
        MapPlutusData properties = parts.properties();
        if (!isNested(parts)) {
            return Optional.of(properties);
        }
        MapPlutusData byPolicy = (MapPlutusData) properties.getMap().get(NESTED_MAP_KEY);

        if (referenceNft == null) {
            // No asset context: only an unambiguous single entry can be resolved
            return singleValue(byPolicy).flatMap(Cip68DatumParser::singleValue);
        }
        String assetNameWithoutLabel = referenceNft.assetName().substring(Cip68Constants.REFERENCE_TOKEN_PREFIX.length());
        return asMap(byPolicy.getMap().get(BytesPlutusData.of(HexUtil.decodeHexString(referenceNft.policyId()))))
                .flatMap(byAsset -> asMap(byAsset.getMap().get(BytesPlutusData.of(HexUtil.decodeHexString(assetNameWithoutLabel)))));
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
    private ParsedCip68Datum buildParsedDatum(DatumParts parts, @Nullable AssetType referenceNft) {
        List<ValidationError> errors = new ArrayList<>();
        Long version = readVersion(parts, errors);

        Optional<MapPlutusData> metadata = resolveMetadata(parts, referenceNft);
        if (metadata.isEmpty()) {
            errors.add(ValidationError.mandatory(CIP_68, null, "721", NO_NESTED_ENTRY,
                    referenceNft != null
                            ? "the nested \"721\" metadata has no usable entry for this token (a map under its policy id and asset name)"
                            : "the nested \"721\" metadata has no single entry to read without a token"));
            return new ParsedCip68Datum(null, null, null, null, null, null, version, null, null, null, errors);
        }
        MapPlutusData properties = metadata.get();

        Long decimals = readDecimals(properties, errors);
        String description = readText(DESCRIPTION, properties, errors);
        String logo = readUri(LOGO, properties, errors);
        String name = readText(NAME, properties, errors);
        String ticker = readText(TICKER, properties, errors);
        String url = readText(URL, properties, errors);
        String image = readUri(IMAGE, properties, errors);
        String mediaType = readText(MEDIA_TYPE, properties, errors);
        Map<String, Object> json = buildPropertiesJson(properties, errors);
        return new ParsedCip68Datum(decimals, description, logo, name, ticker, url, version, image, mediaType, json, errors);
    }

    /**
     * The version is stored as a long, but a datum integer is unbounded: a value that does not fit is stored as no
     * version (and reported), instead of letting {@code longValue()} silently wrap it ({@code 2^64 + 1} would become 1).
     * A version that CIP-68 does not define is stored as it is, with an error.
     */
    @Nullable
    private Long readVersion(DatumParts parts, List<ValidationError> errors) {
        BigInteger value = parts.version();
        if (value == null) {
            // the CIP requires the version: a datum without an integer one is read and flagged, not hidden
            errors.add(ABSENT.equals(parts.versionProblem())
                    ? ValidationError.mandatory(CIP_68, null, "version", ValidationCode.MISSING_VALUE, "version field is missing a mandatory value")
                    : ValidationError.mandatory(CIP_68, null, "version", WRONG_TYPE,
                            "version is " + parts.versionProblem() + ", not an integer, stored without a version"));
            return null;
        }
        if (value.bitLength() >= Long.SIZE) {
            errors.add(ValidationError.mandatory(CIP_68, null, "version", OUT_OF_RANGE,
                    "version " + abbreviate(value.toString()) + " does not fit a 64-bit integer, stored without a version"));
            return null;
        }
        long version = value.longValue();
        if (version < MIN_DEFINED_VERSION || version > MAX_DEFINED_VERSION) {
            errors.add(ValidationError.mandatory(CIP_68, null, "version", UNDEFINED_VERSION,
                    "version " + version + " is not one CIP-68 defines (" + MIN_DEFINED_VERSION + " to " + MAX_DEFINED_VERSION + ")"));
        }
        return version;
    }

    /**
     * Combine {@code files[]} and any non-well-known keys into the single JSONB-backed
     * {@code properties} column. Returns {@code null} if neither part is populated, so
     * pure FT rows don't materialise an empty wrapper.
     */
    @Nullable
    private Map<String, Object> buildPropertiesJson(MapPlutusData properties, List<ValidationError> errors) {
        List<Object> files = parseFiles(properties, errors);
        Map<String, Object> additional = readEntries(properties, "properties.", true, errors);

        boolean hasFiles = !files.isEmpty();
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

    /** Internal record for the unwrapped CIP-68 envelope (properties Map, version as written). */
    private record DatumParts(MapPlutusData properties, @Nullable BigInteger version, @Nullable String versionProblem) {}

    /** {@code versionProblem} of a datum that has no second field. */
    private static final String ABSENT = "absent";

    private static String typeOf(PlutusData data) {
        if (data instanceof BytesPlutusData) {
            return "bytes";
        } else if (data instanceof MapPlutusData) {
            return "a map";
        } else if (data instanceof ListPlutusData) {
            return "a list";
        } else if (data instanceof ConstrPlutusData) {
            return "a constructor";
        }
        return "not an integer";
    }

    /**
     * Reads a text property. CIP-68 says text is UTF-8, so valid UTF-8 is stored as text. Bytes that are
     * not valid UTF-8 are stored as hex, the same rule as for additional properties, instead of being
     * decoded with replacement characters, which would lose the original bytes; the error says so. A value that is
     * not a byte string is left out, with an error.
     */
    @Nullable
    private String readText(String propertyName, MapPlutusData map, List<ValidationError> errors) {
        PlutusData property = map.getMap().get(BytesPlutusData.of(propertyName));
        if (property == null) {
            return null;
        }
        if (!(property instanceof BytesPlutusData bytes)) {
            errors.add(wrongType(propertyName));
            return null;
        }
        return typedBytesToText(propertyName, bytes.getValue(), errors);
    }

    /**
     * Reads a CIP-68 {@code uri} ({@code uri = bounded_bytes / [* bounded_bytes]}), used for the NFT
     * {@code image} and the FT {@code logo}: a value longer than 64 bytes, the most a Plutus byte string
     * holds, is split into a list of byte-string chunks, and this joins them back together. The chunks are
     * joined as bytes and decoded once, so a multi-byte character cut by a chunk boundary survives.
     * Elements of the list that are not byte strings are ignored. There is no size cap: a datum is at most one
     * transaction, 16 KB. The scheme is checked by the validator.
     */
    @Nullable
    private String readUri(String propertyName, MapPlutusData map, List<ValidationError> errors) {
        PlutusData property = map.getMap().get(BytesPlutusData.of(propertyName));
        byte[] value = switch (property) {
            case BytesPlutusData bytes -> bytes.getValue();
            case ListPlutusData list -> joinChunks(list);
            case null -> null;
            default -> {
                errors.add(wrongType(propertyName));
                yield null;
            }
        };
        if (value == null) {
            return null;
        }
        // a single byte string keeps an empty value ("" is what some NFTs declare); an empty list is nothing
        return property instanceof ListPlutusData && value.length == 0 ? null : typedBytesToText(propertyName, value, errors);
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

    private static String typedBytesToText(String propertyName, byte[] bytes, List<ValidationError> errors) {
        if (!StringUtil.isValidUTF8(bytes)) {
            errors.add(ValidationError.optional(CIP_68, null, propertyName, NOT_UTF8,
                    propertyName + " is not valid UTF-8, stored as hex"));
        }
        return bytesToText(bytes);
    }

    /** {@code name} is required by every label; the rest of the typed fields are required, if at all, by some labels only. */
    private static ValidationError wrongType(String propertyName) {
        String error = propertyName + " is not a byte string, left out";
        return NAME.equals(propertyName)
                ? ValidationError.mandatory(CIP_68, null, propertyName, WRONG_TYPE, error)
                : ValidationError.optional(CIP_68, null, propertyName, WRONG_TYPE, error);
    }

    /**
     * Reads {@code decimals} from the datum. The value is an unbounded on-chain integer, so a value that doesn't fit
     * in a {@code long} is stored as no value (and reported) instead of being narrowed: {@code longValue()} alone
     * would silently wrap values above {@code 2^63} into a plausible-looking number. A value that fits a long but is
     * outside the range CIP-68 allows is stored as written, with an error.
     */
    @Nullable
    private Long readDecimals(MapPlutusData map, List<ValidationError> errors) {
        PlutusData property = map.getMap().get(BytesPlutusData.of(DECIMALS));
        if (property == null) {
            return null;
        }
        if (!(property instanceof BigIntPlutusData bigInt)) {
            errors.add(ValidationError.optional(CIP_68, null, DECIMALS, WRONG_TYPE, "decimals is not an integer, left out"));
            return null;
        }

        BigInteger value = bigInt.getValue();
        // bitLength guard first: longValue() is only exact when the value fits in a long
        if (value.bitLength() >= Long.SIZE) {
            errors.add(ValidationError.optional(CIP_68, null, DECIMALS, OUT_OF_RANGE,
                    "decimals " + abbreviate(value.toString()) + " is outside the range CIP-68 allows " + TokenDecimals.RANGE
                            + ", left out"));
            return null;
        }
        if (!TokenDecimals.isInRange(value.longValue())) {
            errors.add(ValidationError.optional(CIP_68, null, DECIMALS, OUT_OF_RANGE,
                    "decimals " + value + " is outside the range CIP-68 allows " + TokenDecimals.RANGE));
        }
        return value.longValue();
    }

    /**
     * Walk the {@code files} key (if present) and return a list of file descriptors, as written.
     * Each element is a {@code Map<String, Object>} with keys {@code name}, {@code mediaType},
     * {@code src} where present. Unknown keys inside a file entry are preserved verbatim.
     * <p>
     * {@code files} is optional. An entry that breaks the CIP-68 definition is kept, with one error for it: every
     * entry has to be a map with a {@code mediaType} byte string and a {@code src} that is a URI with one of the
     * allowed schemes. Returns an empty list when there is no {@code files}, never {@code null}.
     */
    private List<Object> parseFiles(MapPlutusData properties, List<ValidationError> errors) {
        PlutusData filesProp = properties.getMap().get(BytesPlutusData.of(FILES));
        if (!(filesProp instanceof ListPlutusData filesList)) {
            if (filesProp != null) {
                errors.add(ValidationError.optional(CIP_68, null, FILES, WRONG_TYPE, "files is not a list, left out"));
            }
            return List.of();
        }
        List<Object> result = new ArrayList<>();
        int index = 0;
        for (PlutusData item : filesList.getPlutusDataList()) {
            invalidFileReason(item).ifPresent(reason ->
                    errors.add(ValidationError.optional(CIP_68, null, FILES, INVALID_FILES_ENTRY, reason)));
            if (item instanceof MapPlutusData fileMap) {
                Map<String, Object> file = readEntries(fileMap, "files[].", false, errors);
                if (!file.isEmpty()) {
                    result.add(file);
                }
            } else {
                Object value = unwrapPlutusValue(item, "files[" + index + "]", errors);
                if (value != null) {
                    result.add(value);
                }
            }
            index++;
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
        // reading the src here must not add errors of its own: this only decides whether the entry is valid
        String src = readUri("src", file, new ArrayList<>());
        if (src == null || src.isBlank()) {
            return Optional.of("an entry of 'files' has no usable src");
        }
        if (!Cip68Uri.hasAllowedScheme(src)) {
            return Optional.of("the src '" + abbreviate(src) + "' of an entry of 'files' is not a URI with one of the schemes CIP-68 allows ("
                    + Cip68Uri.ALLOWED_SCHEMES + ")");
        }
        return Optional.empty();
    }

    /**
     * Converts the entries of a metadata map to JSON-ready values, keyed by the text of the key. {@code prefix} names
     * the map in errors ({@code properties.} for additional properties, {@code files[].} for the entries of a file);
     * {@code skipTypedKeys} leaves out the keys that have a typed column.
     */
    private Map<String, Object> readEntries(MapPlutusData map, String prefix, boolean skipTypedKeys, List<ValidationError> errors) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<PlutusData, PlutusData> e : byteStringKeysFirst(map)) {
            readEntry(result, e, prefix, skipTypedKeys, errors);
        }
        return result;
    }

    private void readEntry(Map<String, Object> result, Map.Entry<PlutusData, PlutusData> entry, String prefix,
                           boolean skipTypedKeys, List<ValidationError> errors) {
        String key = keyText(entry.getKey());
        if (key == null) {
            errors.add(ValidationError.optional(CIP_68, null, prefix + "*", UNSUPPORTED_KEY,
                    "an entry has a " + keyKind(entry.getKey()) + " as key, which has no text form, left out"));
            return;
        }
        if (skipTypedKeys && TYPED_KEYS.contains(key)) {
            return;
        }
        String field = prefix + abbreviate(key);
        // a property nested this deep is an obvious attack: it is dropped, and so is nothing else. The check is
        // bounded, while unwrapping recurses as deep as the value goes, so it comes first.
        if (dropsTooDeepProperty(field, entry.getValue(), errors)) {
            return;
        }
        Object value = unwrapPlutusValue(entry.getValue(), field, errors);
        if (value != null) {
            putFirst(result, key, value, field, errors);
        }
    }

    /**
     * The text of a map key. The CIP-68 definition allows any metadata as a key ({@code { * metadata => metadata }}),
     * and JSON needs text keys, so a byte string key is read as text or hex (like any byte string) and an integer key
     * as its decimal string, which is what Lucid and Blockfrost do. A list, map or constructor key has no sensible text
     * form: {@code null}, and the entry is left out.
     */
    @Nullable
    private static String keyText(@Nullable PlutusData key) {
        return switch (key) {
            case BytesPlutusData bytes -> bytesToText(bytes.getValue());
            case BigIntPlutusData integer -> integer.getValue().toString();
            case null, default -> null;
        };
    }

    private static String keyKind(@Nullable PlutusData key) {
        return switch (key) {
            case ListPlutusData ignored -> "list";
            case MapPlutusData ignored -> "map";
            case ConstrPlutusData ignored -> "constructor";
            case null, default -> "value of an unsupported kind";
        };
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
    private void putFirst(Map<String, Object> target, String key, Object value, String field, List<ValidationError> errors) {
        if (target.putIfAbsent(key, value) != null) {
            errors.add(ValidationError.optional(CIP_68, null, field, KEY_COLLISION,
                    "two keys read as '" + abbreviate(key) + "', kept the byte string key"));
        }
    }

    /**
     * Drops a property whose value is nested deeper than {@link #MAX_PROPERTY_DEPTH}: that is an obvious attack on the
     * indexer (https://github.com/bloxbean/yaci-store/issues/1240), not metadata. A count, and an error so that the owner is
     * told (and the processor logs one warning that names the token and the transaction); the rest of the datum is indexed.
     */
    private boolean dropsTooDeepProperty(String field, PlutusData value, List<ValidationError> errors) {
        if (!isNestedDeeperThan(value, MAX_PROPERTY_DEPTH)) {
            return false;
        }
        // the processor logs the WARN, since it knows the token and the transaction; the parser only counts it
        metrics.propertyDropped(Cip68Metrics.TOO_DEEP);
        errors.add(ValidationError.optional(CIP_68, null, field, TOO_DEEP,
                "the value is nested deeper than " + MAX_PROPERTY_DEPTH + " levels, dropped"));
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

    /**
     * Unwrap a Plutus value into a Java type suitable for JSONB serialization. Recurses into maps
     * and lists. Bytes become text via {@link #bytesToText}; ints become {@link BigInteger}. The generic CIP-68
     * definition allows Plutus data of any kind only in {@code extra}, so a constructor is not valid metadata, but it
     * is kept: it is stored as {@code {"constructor": n, "fields": [...]}} and reported once for the property, so the
     * owner can fix it and nothing on-chain is hidden. The depth is bounded by {@link #dropsTooDeepProperty}.
     */
    @Nullable
    private Object unwrapPlutusValue(@Nullable PlutusData data, String field, List<ValidationError> errors) {
        return switch (data) {
            case BytesPlutusData b   -> bytesToText(b.getValue());
            case BigIntPlutusData i  -> i.getValue();
            case ListPlutusData list -> {
                List<Object> items = new ArrayList<>();
                for (PlutusData inner : list.getPlutusDataList()) {
                    Object u = unwrapPlutusValue(inner, field, errors);
                    if (u != null) items.add(u);
                }
                yield items;
            }
            case MapPlutusData map -> {
                Map<String, Object> out = new LinkedHashMap<>();
                for (Map.Entry<PlutusData, PlutusData> entry : byteStringKeysFirst(map)) {
                    String key = keyText(entry.getKey());
                    if (key == null) {
                        errors.add(ValidationError.optional(CIP_68, null, field, UNSUPPORTED_KEY,
                                "a nested entry has a " + keyKind(entry.getKey()) + " as key, which has no text form, left out"));
                        continue;
                    }
                    Object u = unwrapPlutusValue(entry.getValue(), field, errors);
                    if (u != null) out.putIfAbsent(key, u);
                }
                yield out;
            }
            case ConstrPlutusData constr -> {
                reportConstructorOnce(field, errors);
                List<Object> fields = new ArrayList<>();
                for (PlutusData inner : constr.getData().getPlutusDataList()) {
                    Object u = unwrapPlutusValue(inner, field, errors);
                    if (u != null) fields.add(u);
                }
                Map<String, Object> out = new LinkedHashMap<>();
                out.put("constructor", constr.getAlternative());
                out.put("fields", fields);
                yield out;
            }
            case null -> null;
            default -> null;
        };
    }

    private static void reportConstructorOnce(String field, List<ValidationError> errors) {
        boolean reported = errors.stream().anyMatch(e -> PLUTUS_CONSTRUCTOR.equals(e.code()) && field.equals(e.fieldName()));
        if (!reported) {
            errors.add(ValidationError.optional(CIP_68, null, field, PLUTUS_CONSTRUCTOR,
                    "the value holds a Plutus constructor, which CIP-68 metadata does not allow, stored as {constructor, fields}"));
        }
    }

    private static String describe(@Nullable AssetType referenceNft) {
        return referenceNft == null ? "an unknown reference NFT" : referenceNft.policyId() + "/" + referenceNft.assetName();
    }

    /** A datum that is an attack can be 16 KB of nesting: the log shows its start only. */
    private static String abbreviateDatum(String datum) {
        return datum.length() <= 120 ? datum : datum.substring(0, 120) + "...";
    }

    private static String abbreviate(String value) {
        String oneLine = value.replaceAll("\\s+", " ");
        return oneLine.length() <= 60 ? oneLine : oneLine.substring(0, 60) + "...";
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

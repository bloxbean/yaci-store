package com.bloxbean.cardano.yaci.store.extensions.assetstore.validation;

/**
 * The {@code code} of a {@link ValidationError}: a stable reason for programs. New codes may be added; an existing one is never changed or
 * removed. {@link #DOCUMENTATION} is shown in the OpenAPI description of {@code validation_errors}, and a test keeps it in step with these
 * constants.
 */
public final class ValidationCode {

    public static final String MISSING_VALUE = "missing_value";
    public static final String WRONG_TYPE = "wrong_type";
    public static final String INVALID_URI_SCHEME = "invalid_uri_scheme";
    public static final String INVALID_LENGTH = "invalid_length";
    public static final String INVALID_VALUE = "invalid_value";
    public static final String OUT_OF_RANGE = "out_of_range";
    public static final String INVALID_VERSION = "invalid_version";
    public static final String PLUTUS_CONSTRUCTOR = "plutus_constructor";
    public static final String NOT_UTF8 = "not_utf8";
    public static final String INVALID_FILES_ENTRY = "invalid_files_entry";
    public static final String NO_NESTED_ENTRY = "no_nested_entry";
    public static final String TOO_DEEP = "too_deep";
    public static final String UNSUPPORTED_KEY = "unsupported_key";
    public static final String KEY_COLLISION = "key_collision";
    public static final String INVALID_BASE64 = "invalid_base64";
    public static final String TOO_LARGE = "too_large";

    /** Markdown, for the OpenAPI description. */
    public static final String DOCUMENTATION = """
            | code | meaning |
            |---|---|
            | `missing_value` | a field the CIP requires is missing or empty: for CIP-68 the `version` and, as the label requires it, `name`, `description` (333) and `image` (222, 444); for CIP-26 `name` and `description` |
            | `wrong_type` | the value has a type the CIP does not allow (for example a number where text is expected); a value that cannot be kept is left out |
            | `invalid_uri_scheme` | an `image` or `logo` is not a URI with one of the schemes CIP-68 allows (`https`, `ipfs`, `ar`, `data`); the value is kept as written |
            | `invalid_length` | a CIP-26 text is shorter or longer than CIP-26 allows (for example a `name` over 50 characters) |
            | `invalid_value` | a CIP-26 value breaks a rule of the registry's validator that is not a type, a length or a range |
            | `out_of_range` | a number is outside the range allowed: `decimals` |
            | `invalid_version` | the CIP-68 `version` is there but cannot be used: it is not an integer, it does not fit a 64-bit integer (stored without a version), or it is not one CIP-68 defines (1 to 4; the datum is read by its structure and kept). The message says which. A missing `version` is `missing_value` |
            | `plutus_constructor` | a property holds a Plutus constructor, which CIP-68 metadata does not allow; it is kept as `{constructor, fields}` |
            | `not_utf8` | a text is not valid UTF-8; it is kept as hex |
            | `invalid_files_entry` | an entry of `files` breaks the CIP-68 definition (for example no `mediaType` or `src`); the entry is kept |
            | `no_nested_entry` | a nested datum (`"721"`) has no usable entry for the token (field `721`); the token is indexed without metadata |
            | `too_deep` | a property is nested more than 100 levels and is dropped, the rest of the datum is kept |
            | `unsupported_key` | a key of a map has no text form (a list or a map) and is left out |
            | `key_collision` | two keys read the same (for example the text `1` and the integer `1`); one of them is kept |
            | `invalid_base64` | a CIP-26 `logo` is not valid base64 |
            | `too_large` | a CIP-26 `logo` is over 64 KiB once decoded (kept up to about 1 MiB; anything larger is dropped) |
            """;

    private ValidationCode() {
    }
}

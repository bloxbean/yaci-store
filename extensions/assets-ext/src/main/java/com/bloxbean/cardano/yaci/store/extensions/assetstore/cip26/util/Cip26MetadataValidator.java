package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.util;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.model.Mapping;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.model.Cip26Metadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.util.TokenDecimals;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.validation.FieldCategory;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.validation.ValidationError;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.metadatatools.core.cip26.MetadataCreator;
import org.cardanofoundation.metadatatools.core.cip26.ValidationField;
import org.cardanofoundation.metadatatools.core.cip26.ValidationResult;
import org.cardanofoundation.metadatatools.core.cip26.model.Metadata;
import org.cardanofoundation.metadatatools.core.cip26.model.MetadataProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static com.bloxbean.cardano.yaci.store.extensions.assetstore.validation.ValidationError.CIP_26;

/**
 * Checks token metadata fields against the CIP-26 specification, using the
 * <a href="https://github.com/cardano-foundation/cf-token-metadata-registry">cf-tokens-cip26</a> library, and reports
 * how an entry departs from it as {@link ValidationError}s. Nothing is rejected for breaking the CIP: the entry is
 * stored and served as written, with its errors (ADR 0006), so that its owner can fix the registry and a wallet can
 * decide whether to accept the token. An entry is dropped only when it cannot be stored (its subject is not a valid
 * key) or is an obvious attack (a logo far beyond any real size).
 */
@Component
@Slf4j
public class Cip26MetadataValidator {

    public static final String MISSING_VALUE = "missing_value";
    public static final String WRONG_TYPE = "wrong_type";
    public static final String INVALID_LENGTH = "invalid_length";
    public static final String OUT_OF_RANGE = "out_of_range";
    public static final String INVALID_VALUE = "invalid_value";
    public static final String INVALID_BASE64 = "invalid_base64";
    public static final String TOO_LARGE = "too_large";

    /** The CIP-26 {@code subject} column: {@code VARCHAR(120)}, the CIP's own maximum (policy id and asset name, hex). */
    private static final int SUBJECT_MAX_LENGTH = 120;

    /** A subject is at least a policy id, 28 bytes in hex. */
    private static final int SUBJECT_MIN_LENGTH = 56;

    // CIP-26 logo size cap: ~87,400 base64 chars → 65,536 decoded bytes (64 KiB).
    // The decoded size is what matters; base64 is a 4:3 expansion, so we check the
    // decoded byte length directly rather than the encoded length.
    private static final int LOGO_MAX_DECODED_BYTES = 65_536;

    /**
     * Far beyond any real logo (the CIP's limit is 64 KiB): a registry file with a logo this big is an attack on the
     * indexer, not a token, so the logo is dropped with a warning. Anything between the CIP's limit and this is stored
     * with an error.
     */
    static final int LOGO_HARD_CAP_CHARS = 1_400_000;

    /**
     * Checks the metadata of one registry entry. {@code mapping} is the entry as read from the registry: it is needed to
     * tell a {@code decimals} that is not a number from one that is absent, since the entity has no value for either.
     *
     * @return the errors to store with the entry, or why the entry is dropped
     */
    public Cip26Validation validate(Mapping mapping, Cip26Metadata tokenMetadata) {
        String subjectProblem = subjectProblem(tokenMetadata.getSubject());
        if (subjectProblem != null) {
            log.warn("Dropping CIP-26 entry '{}': {}", abbreviate(tokenMetadata.getSubject()), subjectProblem);
            return Cip26Validation.dropped(subjectProblem);
        }

        List<ValidationError> errors = new ArrayList<>();

        // name and description are the two properties CIP-26 requires
        if (tokenMetadata.getName() == null) {
            errors.add(missing("name"));
        }
        if (tokenMetadata.getDescription() == null) {
            errors.add(missing("description"));
        }

        // cf-tokens-cip26 only checks decimals >= 0, and on an int: range-check the stored Long here, before
        // convertToMetadata narrows it (4294967301 would otherwise pass as 5). The value is stored as written.
        Long decimals = tokenMetadata.getDecimals();
        boolean decimalsOutOfRange = decimals != null && !TokenDecimals.isInRange(decimals);
        if (decimalsOutOfRange) {
            errors.add(ValidationError.optional(CIP_26, null, "decimals", OUT_OF_RANGE,
                    "decimals " + decimals + " is not in the range " + TokenDecimals.RANGE));
        }
        if (decimals == null && mapping != null && mapping.decimals() != null && mapping.decimals().value() != null) {
            errors.add(ValidationError.optional(CIP_26, null, "decimals", WRONG_TYPE,
                    "decimals '" + abbreviate(mapping.decimals().value()) + "' is not an integer"));
        }

        try {
            Metadata cip26Metadata = convertToMetadata(tokenMetadata, decimalsOutOfRange);
            ValidationResult validationResult = MetadataCreator.validateMetadata(cip26Metadata);
            if (!validationResult.isValid()) {
                validationResult.getValidationErrors().forEach(error -> {
                    // the library reports a missing name or description as REQUIRED_PROPERTIES; they are reported above
                    if (error.getField() != ValidationField.REQUIRED_PROPERTIES) {
                        errors.add(fromLibrary(error.getField(), error.getMessage()));
                    }
                });
            }
        } catch (Exception e) {
            // not a way an entry departs from the CIP: something the validator did not foresee
            log.error("Error during CIP-26 validation for subject '{}': {}", tokenMetadata.getSubject(), e.getMessage(), e);
            return Cip26Validation.dropped("validation failed unexpectedly: " + e.getMessage());
        }

        if (!errors.isEmpty()) {
            log.debug("CIP-26 entry '{}' is stored with {} validation errors: {}", tokenMetadata.getSubject(), errors.size(), errors);
        }
        return Cip26Validation.of(errors);
    }

    /**
     * Checks a logo against CIP-26: it has to be valid base64 and its decoded size at most {@value #LOGO_MAX_DECODED_BYTES}
     * bytes. A logo that fails is kept as written, with an error; only one far beyond any real logo is dropped.
     * Image format (PNG/SVG/etc.) is intentionally NOT checked: CIP-26 currently recommends PNG but the spec may
     * broaden, and this should not be stricter than upstream cf-tokens-cip26 on a format dimension.
     *
     * @param subject the token subject (for logging)
     * @param logo    the logo string to check (can be null)
     */
    public Cip26Validation validateLogo(String subject, @Nullable String logo) {
        if (logo == null || logo.isEmpty()) {
            return Cip26Validation.of(List.of());
        }
        if (logo.length() > LOGO_HARD_CAP_CHARS) {
            log.warn("Dropping the logo of CIP-26 entry '{}': {} characters, far beyond the {} bytes CIP-26 allows, "
                    + "this looks like an attack", subject, logo.length(), LOGO_MAX_DECODED_BYTES);
            return Cip26Validation.dropped("the logo has " + logo.length() + " characters");
        }

        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(logo);
        } catch (IllegalArgumentException e) {
            return Cip26Validation.of(List.of(ValidationError.optional(CIP_26, null, "logo", INVALID_BASE64,
                    "logo is not valid base64")));
        }

        if (decoded.length > LOGO_MAX_DECODED_BYTES) {
            return Cip26Validation.of(List.of(ValidationError.optional(CIP_26, null, "logo", TOO_LARGE,
                    "logo is " + decoded.length + " bytes, CIP-26 allows at most " + LOGO_MAX_DECODED_BYTES)));
        }
        return Cip26Validation.of(List.of());
    }

    /** Why a subject cannot be the key of an entry, or {@code null} when it can. */
    @Nullable
    private static String subjectProblem(@Nullable String subject) {
        if (subject == null || subject.isBlank()) {
            return "the subject is missing or blank";
        }
        if (subject.length() % 2 != 0 || !subject.matches("[0-9a-fA-F]+")) {
            return "the subject is not an even-length hex string";
        }
        if (subject.length() < SUBJECT_MIN_LENGTH || subject.length() > SUBJECT_MAX_LENGTH) {
            return "the subject has " + subject.length() + " characters, a subject has " + SUBJECT_MIN_LENGTH + " to "
                    + SUBJECT_MAX_LENGTH;
        }
        return null;
    }

    /** {@code name} and {@code description} are the properties CIP-26 requires; the rest are optional. */
    private static FieldCategory categoryOf(String field) {
        return "name".equals(field) || "description".equals(field) ? FieldCategory.MANDATORY : FieldCategory.OPTIONAL;
    }

    private static ValidationError missing(String field) {
        return ValidationError.mandatory(CIP_26, null, field, MISSING_VALUE, field + " field is missing a mandatory value");
    }

    /** One error of the cf-tokens-cip26 library, as a {@link ValidationError} of the field it is about. */
    private static ValidationError fromLibrary(ValidationField field, String message) {
        String name = field.getKey();
        String text = message.replaceFirst("^property [^:]*: ", "").trim();
        String code;
        if (text.contains("not of expected type")) {
            code = WRONG_TYPE;
        } else if (field == ValidationField.DECIMALS) {
            code = OUT_OF_RANGE;
        } else if (text.contains("characters") || text.contains("length")) {
            code = INVALID_LENGTH;
        } else {
            code = INVALID_VALUE;
        }
        return new ValidationError(CIP_26, null, name, categoryOf(name), code, name + ": " + text);
    }

    private static String abbreviate(@Nullable String value) {
        if (value == null) {
            return "null";
        }
        String oneLine = value.replaceAll("\\s+", " ");
        return oneLine.length() <= 60 ? oneLine : oneLine.substring(0, 60) + "...";
    }

    private Metadata convertToMetadata(Cip26Metadata tokenMetadata, boolean skipDecimals) {
        Metadata metadata = new Metadata();

        if (tokenMetadata.getSubject() != null) {
            metadata.setSubject(tokenMetadata.getSubject());
        }
        if (tokenMetadata.getName() != null) {
            metadata.addProperty(ValidationField.NAME, new MetadataProperty<>(tokenMetadata.getName()));
        }
        if (tokenMetadata.getDescription() != null) {
            metadata.addProperty(ValidationField.DESCRIPTION, new MetadataProperty<>(tokenMetadata.getDescription()));
        }
        if (tokenMetadata.getTicker() != null) {
            metadata.addProperty(ValidationField.TICKER, new MetadataProperty<>(tokenMetadata.getTicker()));
        }
        if (tokenMetadata.getDecimals() != null && !skipDecimals) {
            metadata.addProperty(ValidationField.DECIMALS, new MetadataProperty<>(tokenMetadata.getDecimals().intValue()));
        }
        if (tokenMetadata.getUrl() != null) {
            metadata.addProperty(ValidationField.URL, new MetadataProperty<>(tokenMetadata.getUrl()));
        }

        return metadata;
    }
}

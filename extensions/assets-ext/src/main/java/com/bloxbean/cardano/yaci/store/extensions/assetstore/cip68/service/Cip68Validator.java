package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.service;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.Cip68Uri;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.ParsedCip68Datum;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.validation.ValidationError;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.Cip68Constants.LABEL_FT;
import static com.bloxbean.cardano.yaci.store.extensions.assetstore.validation.ValidationError.CIP_68;

/**
 * Checks a parsed CIP-68 datum against what CIP-68 requires of the label of the token, and reports what is
 * missing or wrong. Nothing is rejected: the token is indexed and served either way, with these errors (ADR 0006), so
 * that its owner can fix them and a wallet can decide whether to accept the token.
 * <ul>
 *   <li>{@code name} for every label;</li>
 *   <li>{@code description} for the 333 fungible token only (for 222 and 444 it is {@code ? description});</li>
 *   <li>{@code image} for the 222 NFT and the 444 RFT, as a URI whose scheme is one of {@code https}, {@code ipfs},
 *       {@code ar} or {@code data}. A missing or empty image, or another scheme (for example {@code iagon://}, or free
 *       text), is an error: wallets and explorers follow the CIP and could not show such a token.</li>
 * </ul>
 * The optional {@code logo} must also be such a URI; that does not depend on the label.
 */
public final class Cip68Validator {

    public static final String MISSING_VALUE = "missing_value";
    public static final String INVALID_URI_SCHEME = "invalid_uri_scheme";

    private Cip68Validator() {
    }

    /**
     * The errors of the fields {@code label} requires. A reference NFT with user tokens of several labels is checked
     * once per label, and each error carries the label it was found for.
     *
     * @param parsed the parsed datum
     * @param label  the user-token label the datum belongs to: {@code 222} (NFT), {@code 333} (FT) or {@code 444} (RFT)
     */
    public static List<ValidationError> validate(ParsedCip68Datum parsed, int label) {
        String subStandard = String.valueOf(label);
        List<ValidationError> errors = new ArrayList<>();
        // a name of the wrong type is already reported by the parser as mandatory, so it is not reported as missing too
        if (parsed.name() == null && !reportedByParser(parsed, "name")) {
            errors.add(missing(subStandard, "name"));
        }
        if (label == LABEL_FT) {
            if (parsed.description() == null) {
                errors.add(missing(subStandard, "description"));
            }
            return errors;
        }
        String image = parsed.image();
        if (image == null || image.isBlank()) {
            errors.add(missing(subStandard, "image"));
        } else if (!Cip68Uri.hasAllowedScheme(image)) {
            errors.add(ValidationError.mandatory(CIP_68, subStandard, "image", INVALID_URI_SCHEME,
                    "image '" + abbreviate(image) + "' is not a URI with one of the schemes CIP-68 allows (https, ipfs, ar, data)"));
        }
        return errors;
    }

    /**
     * The error of a {@code logo} that is not a URI with a scheme CIP-68 allows (a bare IPFS hash, {@code iagon://}, a
     * base64 string). The logo is optional, so it is kept as written and reported; it does not depend on the label.
     */
    @Nullable
    public static ValidationError validateLogo(ParsedCip68Datum parsed) {
        String logo = parsed.logo();
        if (logo == null || logo.isBlank() || Cip68Uri.hasAllowedScheme(logo)) {
            return null;
        }
        return ValidationError.optional(CIP_68, null, "logo", INVALID_URI_SCHEME,
                "logo '" + abbreviate(logo) + "' is not a URI with one of the schemes CIP-68 allows (https, ipfs, ar, data)");
    }

    private static boolean reportedByParser(ParsedCip68Datum parsed, String field) {
        return parsed.validationErrors().stream().anyMatch(e -> field.equals(e.fieldName()));
    }

    private static ValidationError missing(String subStandard, String field) {
        return ValidationError.mandatory(CIP_68, subStandard, field, MISSING_VALUE,
                field + " field is missing a mandatory value");
    }

    private static String abbreviate(String value) {
        String oneLine = value.replaceAll("\\s+", " ");
        return oneLine.length() <= 60 ? oneLine : oneLine.substring(0, 60) + "...";
    }
}

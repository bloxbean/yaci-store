package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.service;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.Cip68Uri;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.ParsedCip68Datum;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.model.Cip68Metadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.validation.FieldCategory;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.validation.ValidationCode;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.validation.ValidationError;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.Cip68Constants.LABEL_FT;
import static com.bloxbean.cardano.yaci.store.extensions.assetstore.validation.ValidationError.CIP_68;

/**
 * Checks a CIP-68 datum against what CIP-68 requires of the label of the token, and reports what is missing or wrong.
 * Nothing is rejected: the token is indexed and served either way, with these errors (ADR 0006), so that its owner can
 * fix them and a wallet can decide whether to accept the token.
 * <p>
 * Which label a reference NFT belongs to is only known for sure when it is asked for: an update of its datum is often
 * made without its user token, and then the indexer can only guess the label, and one reference NFT can serve a 333 and
 * a 444 token at the same time. So what is wrong with the datum itself is stored when it is indexed, and what the label
 * requires is checked when the token is served, for the label of the subject asked for ({@link #validate}):
 * <ul>
 *   <li>{@code name} for every label;</li>
 *   <li>{@code description} for the 333 fungible token only (for 222 and 444 it is {@code ? description}). A
 *       description of the wrong type, given as chunks, or not valid UTF-8, is reported with the category of the label
 *       asked for;</li>
 *   <li>{@code image} for the 222 NFT and the 444 RFT, as a URI whose scheme is one of {@code https}, {@code ipfs},
 *       {@code ar} or {@code data}. A missing or empty image, or another scheme (for example {@code iagon://}, or free
 *       text), is an error: wallets and explorers follow the CIP and could not show such a token.</li>
 * </ul>
 * The optional {@code logo} must also be such a URI; that does not depend on the label, and is checked when the datum is
 * indexed ({@link #validateLogo}).
 */
public final class Cip68Validator {

    public static final String MISSING_VALUE = ValidationCode.MISSING_VALUE;
    public static final String INVALID_URI_SCHEME = ValidationCode.INVALID_URI_SCHEME;

    private static final String NAME = "name";
    private static final String DESCRIPTION = "description";

    private Cip68Validator() {
    }

    /**
     * The errors of a stored datum served as a token of {@code label}: what is wrong with the datum itself, stored when
     * it was indexed, then what {@code label} requires of it. Each error of the label carries the label as its
     * {@code sub_standard}.
     *
     * @param row   a row indexed with its validation errors (not {@code null}: a row indexed before validation existed
     *              was never checked)
     * @param label the label of the subject asked for: {@code 333} (FT) or {@code 444} (RFT)
     */
    public static List<ValidationError> validate(Cip68Metadata row, int label) {
        List<ValidationError> datumErrors = row.getValidationErrors();
        String subStandard = String.valueOf(label);
        List<ValidationError> errors = new ArrayList<>();
        List<ValidationError> descriptionForms = new ArrayList<>();
        for (ValidationError error : datumErrors) {
            if (isDescriptionForm(error)) {
                descriptionForms.add(error);
            } else {
                errors.add(error);
            }
        }
        // a name of the wrong type is already reported by the parser as mandatory, so it is not reported as missing too
        if (row.getName() == null && datumErrors.stream().noneMatch(e -> NAME.equals(e.fieldName()))) {
            errors.add(missing(subStandard, NAME));
        }
        // the form of a description (a wrong type, chunks, bytes that are not UTF-8) is stored by the parser without a
        // label; it is reported for the label asked for, with its category: only the 333 fungible token requires one
        boolean descriptionRequired = label == LABEL_FT;
        for (ValidationError descriptionForm : descriptionForms) {
            errors.add(new ValidationError(CIP_68, subStandard, DESCRIPTION,
                    descriptionRequired ? FieldCategory.MANDATORY : FieldCategory.OPTIONAL,
                    descriptionForm.code(), descriptionForm.error()));
        }
        if (descriptionForms.isEmpty() && descriptionRequired && row.getDescription() == null) {
            errors.add(missing(subStandard, DESCRIPTION));
        }
        if (label == LABEL_FT) {
            return errors;
        }
        String image = row.getImage();
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

    private static boolean isDescriptionForm(ValidationError error) {
        return DESCRIPTION.equals(error.fieldName())
                && (ValidationCode.WRONG_TYPE.equals(error.code()) || ValidationCode.CHUNKED_TEXT.equals(error.code())
                || ValidationCode.NOT_UTF8.equals(error.code()));
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

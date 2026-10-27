package com.bloxbean.cardano.yaci.store.extensions.assetstore.validation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;

/**
 * One way in which the metadata of a token does not follow its CIP. The token is indexed and served as
 * written; these entries tell the owner what to fix and let a wallet decide whether to accept the token.
 * They are stored as JSON next to the row and returned as {@code validation_errors} by the API.
 *
 * @param cip           the standard the rule comes from, {@link #CIP_26} or {@link #CIP_68}
 * @param subStandard   the CIP-68 label the rule was checked for ({@code "222"}, {@code "333"}, {@code "444"});
 *                      {@code null} for CIP-26 and for rules that do not depend on a label
 * @param fieldName     the metadata field, for example {@code description} or {@code properties.owner}
 * @param fieldCategory whether the CIP requires the field
 * @param code          a stable machine-readable reason, for example {@code missing_value}
 * @param error         a human-readable message
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "A way in which the token's metadata does not follow its CIP. The token is still served.\n\n**Codes**\n\n" + ValidationCode.DOCUMENTATION)
public record ValidationError(
        @JsonProperty("cip") @Schema(description = "The standard the rule comes from.", example = "CIP-68") String cip,
        @JsonProperty("sub_standard") @Nullable
        @Schema(description = "The CIP-68 label the rule was checked for; absent for CIP-26.", example = "333") String subStandard,
        @JsonProperty("field_name") @Schema(description = "The metadata field.", example = "description") String fieldName,
        @JsonProperty("field_category") @Schema(description = "Whether the CIP requires the field.") FieldCategory fieldCategory,
        @JsonProperty("code") @Schema(description = "Stable machine-readable reason. New codes may be added; an existing one is never changed.",
                example = "missing_value",
                allowableValues = {ValidationCode.MISSING_VALUE, ValidationCode.WRONG_TYPE, ValidationCode.INVALID_URI_SCHEME,
                        ValidationCode.INVALID_LENGTH, ValidationCode.INVALID_VALUE, ValidationCode.OUT_OF_RANGE,
                        ValidationCode.INVALID_VERSION, ValidationCode.PLUTUS_CONSTRUCTOR, ValidationCode.NOT_UTF8,
                        ValidationCode.INVALID_FILES_ENTRY, ValidationCode.NO_NESTED_ENTRY, ValidationCode.TOO_DEEP,
                        ValidationCode.UNSUPPORTED_KEY, ValidationCode.KEY_COLLISION, ValidationCode.INVALID_BASE64,
                        ValidationCode.TOO_LARGE, ValidationCode.CHUNKED_TEXT}) String code,
        @JsonProperty("error") @Schema(description = "Human-readable message.") String error) {

    public static final String CIP_26 = "CIP-26";
    public static final String CIP_68 = "CIP-68";

    public static ValidationError mandatory(String cip, @Nullable String subStandard, String fieldName,
                                            String code, String error) {
        return new ValidationError(cip, subStandard, fieldName, FieldCategory.MANDATORY, code, error);
    }

    public static ValidationError optional(String cip, @Nullable String subStandard, String fieldName,
                                           String code, String error) {
        return new ValidationError(cip, subStandard, fieldName, FieldCategory.OPTIONAL, code, error);
    }
}

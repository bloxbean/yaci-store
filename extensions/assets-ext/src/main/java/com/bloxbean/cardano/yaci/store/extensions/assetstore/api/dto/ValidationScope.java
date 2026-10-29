package com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.validation.FieldCategory;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.validation.ValidationError;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * Which tokens a request accepts, by how well their metadata follows its CIP ({@code ?validation=}). A token that is not accepted is
 * left out as if it was not there: {@code 404} for a single subject, absent from a batch. The {@code validation_errors} of an accepted
 * token are never filtered.
 */
public enum ValidationScope {

    /** Every indexed token, whatever is wrong with it. The default. */
    ALL,

    /** Tokens with no {@link FieldCategory#MANDATORY} error; problems with optional fields are tolerated. */
    MANDATORY_ONLY,

    /** Tokens with no error at all, mandatory or optional (the tokens with {@code metadata.valid == true}). */
    STRICT;

    /** Whether a resolved subject is accepted; {@code valid} is the legacy rule for a token that was never checked. */
    public boolean accepts(Subject subject) {
        return accepts(subject.validationErrors(), subject.metadata().valid());
    }

    /**
     * @param errors      the errors of the token, or {@code null} when it was indexed before validation existed (never checked)
     * @param legacyValid the rule such a token falls back to: name and description both present
     */
    public boolean accepts(@Nullable List<ValidationError> errors, boolean legacyValid) {
        if (this == ALL) {
            return true;
        }
        if (errors == null) {
            return legacyValid;
        }
        return this == STRICT
                ? errors.isEmpty()
                : errors.stream().noneMatch(e -> e.fieldCategory() == FieldCategory.MANDATORY);
    }
}

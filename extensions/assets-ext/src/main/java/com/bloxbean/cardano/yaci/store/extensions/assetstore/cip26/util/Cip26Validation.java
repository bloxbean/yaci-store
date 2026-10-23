package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.util;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.validation.ValidationError;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * The outcome of checking a registry entry (or its logo) against CIP-26: the {@link ValidationError}s to store and
 * return with it, and, only for what is an obvious attack or cannot be stored at all, the reason the entry is dropped.
 *
 * @param errors          how the entry departs from CIP-26; the entry is stored and served whatever they are
 * @param byzantineReason why the entry is dropped (a subject that cannot be a key, a logo far beyond any real size), or
 *                        {@code null} when it is kept
 */
public record Cip26Validation(List<ValidationError> errors, @Nullable String byzantineReason) {

    public static Cip26Validation of(List<ValidationError> errors) {
        return new Cip26Validation(errors, null);
    }

    public static Cip26Validation dropped(String reason) {
        return new Cip26Validation(List.of(), reason);
    }

    public boolean isDropped() {
        return byzantineReason != null;
    }
}

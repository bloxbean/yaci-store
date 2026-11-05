package com.bloxbean.cardano.yaci.store.extensions.assetstore.api.service;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.validation.ValidationError;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * Limits the validation errors of a response to the properties the client asked for ({@code ?property=}): a client that asks for the
 * ticker has no use for an error about the image, and its {@code metadata.valid} and {@code ?validation=} should be about what it reads.
 * <ul>
 *   <li>No {@code property} filter, or no errors recorded for the row ({@code null}, indexed before validation existed): unchanged.</li>
 *   <li>An error is kept when its {@code field_name} is one of the requested properties. An {@code image} error also counts for
 *       {@code logo}, since a rich fungible token's image is served as its logo.</li>
 *   <li>The error that says the token has no usable metadata at all ({@code 721}, a nested datum with no entry for it) concerns every
 *       property, so it is always kept.</li>
 *   <li>An error about something the merged endpoint cannot return (the {@code files}, an extra property, a key) is never kept under a filter.</li>
 * </ul>
 */
final class PropertyErrorFilter {

    private static final String NO_NESTED_ENTRY_FIELD = "721";
    private static final String IMAGE = "image";
    private static final String LOGO = "logo";

    private PropertyErrorFilter() {
    }

    @Nullable
    static List<ValidationError> filter(@Nullable List<ValidationError> errors, List<String> properties) {
        if (errors == null || properties.isEmpty()) {
            return errors;
        }
        return errors.stream().filter(error -> concerns(error.fieldName(), properties)).toList();
    }

    private static boolean concerns(String fieldName, List<String> properties) {
        return NO_NESTED_ENTRY_FIELD.equals(fieldName)
                || properties.contains(fieldName)
                || (IMAGE.equals(fieldName) && properties.contains(LOGO));
    }
}

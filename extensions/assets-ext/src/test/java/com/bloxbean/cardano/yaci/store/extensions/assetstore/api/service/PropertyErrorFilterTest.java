package com.bloxbean.cardano.yaci.store.extensions.assetstore.api.service;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.validation.ValidationError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PropertyErrorFilter")
class PropertyErrorFilterTest {

    private static ValidationError error(String field) {
        return ValidationError.optional("CIP-68", null, field, "wrong_type", field + " is wrong");
    }

    private static final ValidationError DESCRIPTION = error("description");
    private static final ValidationError TICKER = error("ticker");
    private static final ValidationError IMAGE = error("image");
    private static final ValidationError FILES = error("files");
    private static final ValidationError EXTRA = error("properties.owner");
    private static final ValidationError NO_ENTRY = ValidationError.mandatory("CIP-68", null, "721", "no_nested_entry", "no entry");

    private static final List<ValidationError> ALL = List.of(DESCRIPTION, TICKER, IMAGE, FILES, EXTRA, NO_ENTRY);

    @Test
    @DisplayName("without a property filter every error is kept")
    void noFilterKeepsEverything() {
        assertThat(PropertyErrorFilter.filter(ALL, List.of())).isEqualTo(ALL);
    }

    @Test
    @DisplayName("a row with no errors recorded (null) stays null, with or without a filter")
    void nullStaysNull() {
        assertThat(PropertyErrorFilter.filter(null, List.of())).isNull();
        assertThat(PropertyErrorFilter.filter(null, List.of("ticker"))).isNull();
    }

    @Test
    @DisplayName("only the errors of the requested properties are kept")
    void keepsTheRequestedProperties() {
        assertThat(PropertyErrorFilter.filter(ALL, List.of("ticker", "description")))
                .containsExactly(DESCRIPTION, TICKER, NO_ENTRY);
    }

    @Test
    @DisplayName("an image error counts for the logo, because a rich fungible token's image is served as its logo")
    void imageCountsForLogo() {
        assertThat(PropertyErrorFilter.filter(ALL, List.of("logo"))).containsExactly(IMAGE, NO_ENTRY);
        assertThat(PropertyErrorFilter.filter(ALL, List.of("ticker"))).doesNotContain(IMAGE);
    }

    @Test
    @DisplayName("the error that says there is no usable metadata is kept whatever is asked for")
    void noNestedEntryIsAlwaysKept() {
        assertThat(PropertyErrorFilter.filter(ALL, List.of("name"))).containsExactly(NO_ENTRY);
    }

    @Test
    @DisplayName("errors about things the merged endpoint cannot return are never kept under a filter")
    void filesAndExtraPropertiesAreHidden() {
        assertThat(PropertyErrorFilter.filter(ALL, List.of("name", "ticker", "description", "decimals", "logo", "url", "version")))
                .doesNotContain(FILES, EXTRA);
    }
}

package com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.validation.ValidationError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ValidationScope")
class ValidationScopeTest {

    private static final ValidationError MANDATORY = ValidationError.mandatory("CIP-68", "333", "description", "missing_value", "m");
    private static final ValidationError OPTIONAL = ValidationError.optional("CIP-68", null, "logo", "invalid_uri_scheme", "o");

    @Test
    @DisplayName("ALL accepts every token, checked or not")
    void allAcceptsEverything() {
        assertThat(ValidationScope.ALL.accepts(List.of(), true)).isTrue();
        assertThat(ValidationScope.ALL.accepts(List.of(OPTIONAL), false)).isTrue();
        assertThat(ValidationScope.ALL.accepts(List.of(MANDATORY, OPTIONAL), false)).isTrue();
        assertThat(ValidationScope.ALL.accepts(null, false)).isTrue();
    }

    @Test
    @DisplayName("MANDATORY_ONLY accepts a token with no mandatory error, optional errors are tolerated")
    void mandatoryOnly() {
        assertThat(ValidationScope.MANDATORY_ONLY.accepts(List.of(), false)).isTrue();
        assertThat(ValidationScope.MANDATORY_ONLY.accepts(List.of(OPTIONAL), false)).isTrue();
        assertThat(ValidationScope.MANDATORY_ONLY.accepts(List.of(MANDATORY), true)).isFalse();
        assertThat(ValidationScope.MANDATORY_ONLY.accepts(List.of(OPTIONAL, MANDATORY), true)).isFalse();
    }

    @Test
    @DisplayName("STRICT accepts only a token with no error at all")
    void strict() {
        assertThat(ValidationScope.STRICT.accepts(List.of(), false)).isTrue();
        assertThat(ValidationScope.STRICT.accepts(List.of(OPTIONAL), true)).isFalse();
        assertThat(ValidationScope.STRICT.accepts(List.of(MANDATORY), true)).isFalse();
    }

    @Test
    @DisplayName("a token that was never checked (null errors) is judged by the legacy rule in MANDATORY_ONLY and STRICT")
    void neverCheckedFallsBackToTheLegacyRule() {
        for (ValidationScope scope : List.of(ValidationScope.MANDATORY_ONLY, ValidationScope.STRICT)) {
            assertThat(scope.accepts(null, true)).as(scope + " with name and description").isTrue();
            assertThat(scope.accepts(null, false)).as(scope + " without them").isFalse();
        }
    }
}

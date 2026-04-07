package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.util;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.model.TokenMetadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("java:S2187") // tests are in @Nested inner classes
@DisplayName("TokenMetadataValidator")
class TokenMetadataValidatorTest {

    // Valid 56-char hex subject (28-byte policy ID + short asset name)
    private static final String VALID_SUBJECT = "025146866af908340247fe4e9672d5ac7059f1e8534696b5f920c9e66362544848";

    private final TokenMetadataValidator validator = new TokenMetadataValidator();

    private static TokenMetadata metadata(String subject, String name, String description) {
        TokenMetadata m = new TokenMetadata();
        m.setSubject(subject);
        m.setName(name);
        m.setDescription(description);
        return m;
    }

    @Nested
    @DisplayName("validate — valid metadata")
    class ValidMetadata {

        @Test
        void acceptsMinimalValidMetadata() {
            TokenMetadata m = metadata(VALID_SUBJECT, "Nutcoin", "A test token");
            assertThat(validator.validate(m)).isTrue();
        }

        @Test
        void acceptsMetadataWithAllOptionalFields() {
            TokenMetadata m = metadata(VALID_SUBJECT, "Nutcoin", "A test token");
            m.setTicker("NUT");
            m.setUrl("https://example.com");
            m.setDecimals(6L);
            assertThat(validator.validate(m)).isTrue();
        }

        @Test
        void acceptsZeroDecimals() {
            TokenMetadata m = metadata(VALID_SUBJECT, "Token", "Description");
            m.setDecimals(0L);
            assertThat(validator.validate(m)).isTrue();
        }

        @Test
        void acceptsMaxDecimals() {
            TokenMetadata m = metadata(VALID_SUBJECT, "Token", "Description");
            m.setDecimals(255L);
            assertThat(validator.validate(m)).isTrue();
        }
    }

    @Nested
    @DisplayName("validate — invalid metadata")
    class InvalidMetadata {

        @Test
        void rejectsMissingName() {
            TokenMetadata m = metadata(VALID_SUBJECT, null, "A description");
            assertThat(validator.validate(m)).isFalse();
        }

        @Test
        void rejectsMissingDescription() {
            TokenMetadata m = metadata(VALID_SUBJECT, "Nutcoin", null);
            assertThat(validator.validate(m)).isFalse();
        }

        @Test
        void rejectsMissingSubject() {
            TokenMetadata m = metadata(null, "Nutcoin", "A description");
            assertThat(validator.validate(m)).isFalse();
        }

        @Test
        void rejectsNameExceedingMaxLength() {
            String longName = "A".repeat(100);
            TokenMetadata m = metadata(VALID_SUBJECT, longName, "A description");
            assertThat(validator.validate(m)).isFalse();
        }

        @Test
        void rejectsTickerExceedingMaxLength() {
            TokenMetadata m = metadata(VALID_SUBJECT, "Token", "Description");
            m.setTicker("ABCDEFGHIJ"); // CIP-26: ticker max 9 chars
            assertThat(validator.validate(m)).isFalse();
        }

        @Test
        void rejectsUrlExceedingMaxLength() {
            TokenMetadata m = metadata(VALID_SUBJECT, "Token", "Description");
            m.setUrl("https://example.com/" + "a".repeat(300)); // CIP-26: url max 250 chars
            assertThat(validator.validate(m)).isFalse();
        }

        @Test
        void rejectsNegativeDecimals() {
            TokenMetadata m = metadata(VALID_SUBJECT, "Token", "Description");
            m.setDecimals(-1L);
            assertThat(validator.validate(m)).isFalse();
        }

        @Test
        void rejectsDecimalsExceeding255() {
            TokenMetadata m = metadata(VALID_SUBJECT, "Token", "Description");
            m.setDecimals(256L);
            assertThat(validator.validate(m)).isFalse();
        }
    }

    @Nested
    @DisplayName("validateLogo")
    class ValidateLogo {

        @Test
        void acceptsNullLogo() {
            assertThat(validator.validateLogo(VALID_SUBJECT, null)).isTrue();
        }

        @Test
        void acceptsEmptyLogo() {
            assertThat(validator.validateLogo(VALID_SUBJECT, "")).isTrue();
        }

        @Test
        void acceptsValidBase64PngLogo() {
            // Minimal valid PNG as base64 (1x1 transparent pixel)
            String validPng = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";
            assertThat(validator.validateLogo(VALID_SUBJECT, validPng)).isTrue();
        }

        @Test
        void rejectsLogoExceedingMaxSize() {
            // CIP-26: logo max 87400 base64 chars (65 KB decoded)
            String oversizedLogo = "A".repeat(90000);
            assertThat(validator.validateLogo(VALID_SUBJECT, oversizedLogo)).isFalse();
        }

        @Test
        void handlesInvalidSubjectGracefully() {
            // Invalid subject should not crash — validation may fail on subject, not logo
            assertThat(validator.validateLogo("invalid", null)).isTrue();
        }
    }
}

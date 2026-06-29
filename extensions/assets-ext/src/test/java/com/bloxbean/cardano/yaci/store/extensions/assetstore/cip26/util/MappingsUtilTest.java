package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.util;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.model.Item;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.model.Mapping;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.model.Cip26Metadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pinned tests for {@link MappingsUtil#toCip26Metadata}.
 *
 * <p>Two paths matter most: the invalid-decimals fallback (a malformed registry entry must
 * not crash the sync — decimals lands as {@code null} and a WARN is logged) and the {@code Item
 * == null} pass-through (any optional field can be missing in a real registry JSON).
 */
@SuppressWarnings("java:S2187") // tests are in @Nested inner classes
@DisplayName("MappingsUtil.toCip26Metadata")
class MappingsUtilTest {

    private static final String SUBJECT = "0011fbab202151eca9e9ef7680569d9419d12e51e693cb05a2edd2ed4341524b";
    private static final String POLICY = "82018201828200581c0c0c6dc5f6b02995465ae5de8bdf07d5466932288bea414614ca7d2a";
    private static final String UPDATED_BY = "registry-bot";
    private static final LocalDateTime UPDATED_AT = LocalDateTime.of(2026, 5, 7, 10, 0);

    private static Item item(String value) {
        return new Item(null, value, null);
    }

    private static Mapping mapping(Item name, Item ticker, Item url, Item decimals, Item logo, Item description) {
        return new Mapping(SUBJECT, url, name, ticker, decimals, logo, POLICY, description);
    }

    @Nested
    @DisplayName("happy path")
    class HappyPath {

        @Test
        void allFieldsPopulatedAreLifted() {
            Mapping m = mapping(
                    item("Cardano Ark Token"),
                    item("CARK"),
                    item("https://example.com"),
                    item("6"),
                    item("base64-logo-bytes"),
                    item("Sample description"));

            Cip26Metadata out = MappingsUtil.toCip26Metadata(m, UPDATED_BY, UPDATED_AT);

            assertThat(out.getSubject()).isEqualTo(SUBJECT);
            assertThat(out.getPolicy()).isEqualTo(POLICY);
            assertThat(out.getName()).isEqualTo("Cardano Ark Token");
            assertThat(out.getTicker()).isEqualTo("CARK");
            assertThat(out.getUrl()).isEqualTo("https://example.com");
            assertThat(out.getDecimals()).isEqualTo(6L);
            assertThat(out.getDescription()).isEqualTo("Sample description");
            assertThat(out.getUpdatedBy()).isEqualTo(UPDATED_BY);
            assertThat(out.getUpdated()).isEqualTo(UPDATED_AT);
            // The full Mapping is also stored verbatim on `properties` so the V2 wire shape can
            // pick up signatures + sequenceNumbers without a re-parse.
            assertThat(out.getProperties()).isSameAs(m);
        }
    }

    @Nested
    @DisplayName("decimals fallback")
    class DecimalsFallback {

        @Test
        void invalidNumericResolvesToNullWithoutThrowing() {
            // CIP-26 registry sometimes contains malformed entries; a sync run must keep
            // going. The util parses decimals from String → Long via Long.valueOf which
            // throws NumberFormatException on garbage; toCip26Metadata catches it and logs.
            Mapping m = mapping(null, null, null, item("not-a-number"), null, null);

            Cip26Metadata out = MappingsUtil.toCip26Metadata(m, UPDATED_BY, UPDATED_AT);

            assertThat(out.getDecimals()).isNull();
        }

        @Test
        void absentDecimalsItemResolvesToNull() {
            // Optional field, absent altogether — common case for tokens that don't declare decimals.
            Mapping m = mapping(item("Token"), null, null, null, null, null);

            Cip26Metadata out = MappingsUtil.toCip26Metadata(m, UPDATED_BY, UPDATED_AT);

            assertThat(out.getDecimals()).isNull();
        }

        @Test
        void itemWithNullValueResolvesToNull() {
            // Item present but its value is explicitly null in the JSON.
            Mapping m = mapping(null, null, null, new Item(null, null, null), null, null);

            Cip26Metadata out = MappingsUtil.toCip26Metadata(m, UPDATED_BY, UPDATED_AT);

            assertThat(out.getDecimals()).isNull();
        }

        @Test
        void validIntegerValueIsParsed() {
            Mapping m = mapping(null, null, null, item("8"), null, null);

            Cip26Metadata out = MappingsUtil.toCip26Metadata(m, UPDATED_BY, UPDATED_AT);

            assertThat(out.getDecimals()).isEqualTo(8L);
        }
    }

    @Nested
    @DisplayName("null Item pass-through")
    class NullItemPassThrough {

        @Test
        void allOptionalFieldsCanBeAbsent() {
            // Minimal valid mapping: only subject/policy. Util must not throw.
            Mapping m = new Mapping(SUBJECT, null, null, null, null, null, POLICY, null);

            Cip26Metadata out = MappingsUtil.toCip26Metadata(m, UPDATED_BY, UPDATED_AT);

            assertThat(out.getSubject()).isEqualTo(SUBJECT);
            assertThat(out.getPolicy()).isEqualTo(POLICY);
            assertThat(out.getName()).isNull();
            assertThat(out.getTicker()).isNull();
            assertThat(out.getUrl()).isNull();
            assertThat(out.getDecimals()).isNull();
            assertThat(out.getDescription()).isNull();
        }

        @Test
        void itemPresentButValueNullResolvesEachFieldToNull() {
            Item nullValue = new Item(null, null, null);
            Mapping m = mapping(nullValue, nullValue, nullValue, nullValue, nullValue, nullValue);

            Cip26Metadata out = MappingsUtil.toCip26Metadata(m, UPDATED_BY, UPDATED_AT);

            assertThat(out.getName()).isNull();
            assertThat(out.getTicker()).isNull();
            assertThat(out.getUrl()).isNull();
            assertThat(out.getDecimals()).isNull();
            assertThat(out.getDescription()).isNull();
        }
    }

    @Nested
    @DisplayName("extractLogo")
    class ExtractLogo {

        @Test
        void returnsNullWhenLogoItemIsAbsent() {
            Mapping m = new Mapping(SUBJECT, null, null, null, null, null, POLICY, null);

            assertThat(MappingsUtil.extractLogo(m)).isNull();
        }

        @Test
        void returnsLogoValueWhenPresent() {
            Mapping m = mapping(null, null, null, null, item("base64-logo"), null);

            assertThat(MappingsUtil.extractLogo(m)).isEqualTo("base64-logo");
        }
    }
}

package com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.cip26;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.model.Item;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.model.Mapping;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.model.Signature;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.model.Cip26Metadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Locks the CIP-26 V2 wire shape produced by {@link Cip26TokenMetadata#from(Cip26Metadata, String)}.
 *
 * <p>This is the factory that builds the {@code subject.standards.cip26} block returned to V2
 * clients. The shape was validated against 7,871 mainnet subjects (May 2026 sweep, 0 yaci-side
 * defects); these tests pin the per-property paths so a future refactor cannot regress that
 * parity silently.
 *
 * <p>The most fragile area is the {@code null} vs {@code []} distinction on {@code signatures}
 * and {@code null} vs explicit-zero on {@code sequenceNumber} — CF preprod is inconsistent
 * (some subjects emit {@code null}, others {@code []}) so the only spec-correct answer is
 * byte-faithful pass-through of whatever the registry source supplied.
 */
@SuppressWarnings("java:S2187") // tests are in @Nested inner classes
@DisplayName("Cip26TokenMetadata.from")
class Cip26TokenMetadataTest {

    private static final String SUBJECT =
            "0011fbab202151eca9e9ef7680569d9419d12e51e693cb05a2edd2ed4341524b";
    private static final String POLICY = "82018201828200581c0c0c6dc5f6b02995465ae5de8bdf07d5466932288bea414614ca7d2a";
    private static final String SIG_HEX = "7880260a00c8a7e7a426f4e971ef938f6dc67a67404dfd4aae3d0a963cf101c3";
    private static final String PUBKEY_HEX = "0c0c6dc5f6b02995465ae5de8bdf07d5466932288bea414614ca7d2a96c63002";
    private static final String LOGO_B64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkAAIAAAUAAeIVWUMAAAAASUVORK5CYII=";

    private static Cip26Metadata entityWith(Mapping mapping) {
        Cip26Metadata e = new Cip26Metadata();
        e.setSubject(SUBJECT);
        e.setPolicy(POLICY);
        e.setProperties(mapping);
        return e;
    }

    private static Item itemNoSig(String value) {
        return new Item(null, value, null);
    }

    private static Item itemEmptySig(String value, int sequenceNumber) {
        return new Item(sequenceNumber, value, List.of());
    }

    private static Item itemWithSig(String value, int sequenceNumber, Signature... sigs) {
        return new Item(sequenceNumber, value, List.of(sigs));
    }

    @Nested
    @DisplayName("entity-level branches")
    class EntityLevel {

        @Test
        void nullEntityReturnsNull() {
            // Defensive contract: callers can pass a missing entity through without a null check.
            assertThat(Cip26TokenMetadata.from(null, null)).isNull();
        }

        @Test
        void entityWithoutMappingExposesSubjectAndPolicyOnly() {
            // Real-world case: a row exists in cip26_metadata but the original mapping JSON wasn't
            // stored (legacy entries). We still want a usable response carrying the on-chain bits.
            Cip26TokenMetadata dto = Cip26TokenMetadata.from(entityWith(null), null);

            assertThat(dto).isNotNull();
            assertThat(dto.getSubject()).isEqualTo(SUBJECT);
            assertThat(dto.getPolicy()).isEqualTo(POLICY);
            assertThat(dto.getName()).isNull();
            assertThat(dto.getDescription()).isNull();
            assertThat(dto.getUrl()).isNull();
            assertThat(dto.getTicker()).isNull();
            assertThat(dto.getDecimals()).isNull();
            assertThat(dto.getLogo()).isNull();
        }

        @Test
        void allWellKnownPropertiesAreLifted() {
            Mapping mapping = new Mapping(
                    SUBJECT,
                    itemNoSig("https://example.com"),
                    itemNoSig("Example Token"),
                    itemNoSig("EXMPL"),
                    itemNoSig("6"),
                    null,
                    POLICY,
                    itemNoSig("Example description"));

            Cip26TokenMetadata dto = Cip26TokenMetadata.from(entityWith(mapping), null);

            assertThat(dto.getName().getValue()).isEqualTo("Example Token");
            assertThat(dto.getDescription().getValue()).isEqualTo("Example description");
            assertThat(dto.getUrl().getValue()).isEqualTo("https://example.com");
            assertThat(dto.getTicker().getValue()).isEqualTo("EXMPL");
            assertThat(dto.getDecimals().getValue()).isEqualByComparingTo(BigDecimal.valueOf(6));
        }

        @Test
        void logoIsSuppressedWhenLogoB64IsNullEvenIfMappingHasLogoItem() {
            // Yaci stores logos in a separate column; if the table copy is absent we don't fabricate
            // one from the mapping JSON alone — that would diverge from CF's wire shape (which gates
            // logo emission on the table-side payload, not the mapping envelope).
            Mapping mapping = new Mapping(SUBJECT, null, null, null, null,
                    itemWithSig("ignored-base64", 0, new Signature(SIG_HEX, PUBKEY_HEX)),
                    POLICY, null);

            Cip26TokenMetadata dto = Cip26TokenMetadata.from(entityWith(mapping), null);

            assertThat(dto.getLogo()).isNull();
        }

        @Test
        void logoUsesTableValueButPreservesMappingSignaturesAndSequence() {
            // Logo is unique: the canonical bytes come from the table column (logoB64) but the
            // signatures/sequenceNumber must come from the mapping's logo Item so off-chain
            // signatures aren't lost when the table is rebuilt.
            Mapping mapping = new Mapping(SUBJECT, null, null, null, null,
                    itemWithSig("ignored-base64", 7, new Signature(SIG_HEX, PUBKEY_HEX)),
                    POLICY, null);

            Cip26TokenMetadata dto = Cip26TokenMetadata.from(entityWith(mapping), LOGO_B64);

            assertThat(dto.getLogo().getValue()).isEqualTo(LOGO_B64);
            assertThat(dto.getLogo().getSignatures())
                    .singleElement()
                    .satisfies(sig -> {
                        assertThat(sig.getSignature()).isEqualTo(SIG_HEX);
                        assertThat(sig.getPublicKey()).isEqualTo(PUBKEY_HEX);
                    });
            assertThat(dto.getLogo().getSequenceNumber()).isEqualByComparingTo(BigDecimal.valueOf(7));
        }

        @Test
        void logoFromTableButNoMappingItemHasNullSignaturesAndSequence() {
            // Defensive: if the mapping JSON has no logo Item at all but the table column does,
            // we still produce a logo property — just without sig metadata.
            Mapping mapping = new Mapping(SUBJECT, null, null, null, null,
                    null, POLICY, null);

            Cip26TokenMetadata dto = Cip26TokenMetadata.from(entityWith(mapping), LOGO_B64);

            assertThat(dto.getLogo().getValue()).isEqualTo(LOGO_B64);
            assertThat(dto.getLogo().getSignatures()).isNull();
            assertThat(dto.getLogo().getSequenceNumber()).isNull();
        }
    }

    @Nested
    @DisplayName("V2 wire-shape — signatures & sequenceNumber")
    class WireShape {

        @Test
        void nullSignaturesPassThroughAsNull() {
            // CF preprod emits "signatures": null when the registry JSON omits the field.
            // yaci's Item.signatures is a null List<Signature> in that case → must remain null.
            Mapping mapping = mappingWithName(itemNoSig("Example"));

            Cip26TokenMetadata dto = Cip26TokenMetadata.from(entityWith(mapping), null);

            assertThat(dto.getName().getSignatures()).isNull();
        }

        @Test
        void emptySignaturesPassThroughAsEmptyList() {
            // CF preprod emits "signatures": [] when the registry JSON has the empty array literal.
            // yaci's Item.signatures is a List.of() in that case → must remain empty list (NOT null).
            // Conflating null and [] reintroduced ~37 mainnet divergences in an earlier fix; this
            // test pins the distinction.
            Mapping mapping = mappingWithName(itemEmptySig("Example", 0));

            Cip26TokenMetadata dto = Cip26TokenMetadata.from(entityWith(mapping), null);

            assertThat(dto.getName().getSignatures()).isEmpty();
        }

        @Test
        void realSignaturesAreMappedToAnnotatedSignaturesVerbatim() {
            Signature sig = new Signature(SIG_HEX, PUBKEY_HEX);
            Mapping mapping = mappingWithName(itemWithSig("Example", 0, sig));

            Cip26TokenMetadata dto = Cip26TokenMetadata.from(entityWith(mapping), null);

            assertThat(dto.getName().getSignatures())
                    .singleElement()
                    .satisfies(annotated -> {
                        assertThat(annotated.getSignature()).isEqualTo(SIG_HEX);
                        assertThat(annotated.getPublicKey()).isEqualTo(PUBKEY_HEX);
                    });
        }

        @Test
        void nullSequenceNumberStaysNull() {
            // CF emits "sequenceNumber": null for entries missing the field. Returning
            // BigDecimal.ZERO instead would falsely advertise "version 0" — a valid sequenceNumber
            // value — so unknown-vs-explicit-zero must be distinguishable on the wire.
            Mapping mapping = mappingWithName(itemNoSig("Example"));

            Cip26TokenMetadata dto = Cip26TokenMetadata.from(entityWith(mapping), null);

            assertThat(dto.getName().getSequenceNumber()).isNull();
        }

        @Test
        void explicitZeroSequenceNumberPropagatesAsBigDecimalZero() {
            // 0 is a valid CIP-26 sequenceNumber (initial registration). Must not be confused
            // with null.
            Mapping mapping = mappingWithName(itemEmptySig("Example", 0));

            Cip26TokenMetadata dto = Cip26TokenMetadata.from(entityWith(mapping), null);

            assertThat(dto.getName().getSequenceNumber()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void nonZeroSequenceNumberPropagatesAsBigDecimal() {
            Mapping mapping = mappingWithName(itemEmptySig("Example", 42));

            Cip26TokenMetadata dto = Cip26TokenMetadata.from(entityWith(mapping), null);

            assertThat(dto.getName().getSequenceNumber()).isEqualByComparingTo(BigDecimal.valueOf(42));
        }

        private Mapping mappingWithName(Item nameItem) {
            return new Mapping(SUBJECT, null, nameItem, null, null, null, POLICY, null);
        }
    }

    @Nested
    @DisplayName("decimals — string-to-BigDecimal conversion")
    class Decimals {

        @Test
        void invalidNumericStringResolvesToNullProperty() {
            // Defensive: CIP-26 schema permits only integer-shaped decimals, but a malformed
            // registry entry shouldn't crash the response. Logged at WARN by the factory.
            Mapping mapping = new Mapping(SUBJECT, null, null, null,
                    itemNoSig("not-a-number"), null, POLICY, null);

            Cip26TokenMetadata dto = Cip26TokenMetadata.from(entityWith(mapping), null);

            assertThat(dto.getDecimals()).isNull();
        }

        @Test
        void validIntegerProducesBigDecimalValue() {
            Mapping mapping = new Mapping(SUBJECT, null, null, null,
                    itemNoSig("8"), null, POLICY, null);

            Cip26TokenMetadata dto = Cip26TokenMetadata.from(entityWith(mapping), null);

            assertThat(dto.getDecimals().getValue()).isEqualByComparingTo(BigDecimal.valueOf(8));
        }

        @Test
        void nullValueItemResolvesToNullProperty() {
            Mapping mapping = new Mapping(SUBJECT, null, null, null,
                    new Item(null, null, null), null, POLICY, null);

            Cip26TokenMetadata dto = Cip26TokenMetadata.from(entityWith(mapping), null);

            assertThat(dto.getDecimals()).isNull();
        }
    }
}

package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.service;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.AssetType;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.FungibleTokenMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.ParsedCip68Datum;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.model.Cip68Metadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.repository.Cip68MetadataRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("java:S2187")
@ExtendWith(MockitoExtension.class)
@DisplayName("Cip68TokenService")
class Cip68TokenServiceTest {

    private static final String POLICY_ID = "577f0b1342f8f8f4aed3388b80a8535812950c7a892495c0ecdf0f1e";
    private static final String BASE_NAME = "464c4454";  // "FLDT" in hex
    private static final String FT_ASSET_NAME = "0014df10" + BASE_NAME;     // fungible token prefix + name
    private static final String REF_ASSET_NAME = "000643b0" + BASE_NAME;    // reference NFT prefix + name
    private static final String NFT_ASSET_NAME = "000de140" + BASE_NAME;    // NFT prefix + name
    private static final String FT_SUBJECT = POLICY_ID + FT_ASSET_NAME;
    private static final String REF_SUBJECT = POLICY_ID + REF_ASSET_NAME;

    @Mock
    private Cip68MetadataRepository repository;

    @InjectMocks
    private Cip68TokenService service;

    @Nested
    @DisplayName("getReferenceNftSubject")
    class GetReferenceNftSubject {

        @Test
        void convertsFungibleTokenSubjectToReferenceNft() {
            Optional<AssetType> result = service.getReferenceNftSubject(FT_SUBJECT);

            assertThat(result).isPresent();
            assertThat(result.get().policyId()).isEqualTo(POLICY_ID);
            assertThat(result.get().assetName()).isEqualTo(REF_ASSET_NAME);
        }

        @Test
        void preservesPolicyId() {
            Optional<AssetType> result = service.getReferenceNftSubject(FT_SUBJECT);

            assertThat(result).isPresent();
            assertThat(result.get().policyId()).isEqualTo(POLICY_ID);
        }

        @Test
        void swapsPrefixButKeepsBaseName() {
            Optional<AssetType> result = service.getReferenceNftSubject(FT_SUBJECT);

            assertThat(result).isPresent();
            // Base name after the 8-char prefix should be identical
            assertThat(result.get().assetName().substring(8)).isEqualTo(BASE_NAME);
            // Prefix should be reference NFT, not fungible token
            assertThat(result.get().assetName().substring(0, 8)).isEqualTo("000643b0");
        }

        @Test
        void returnsEmptyForReferenceNftSubject() {
            // Reference NFT prefix (000643b0) is not a fungible token — should return empty
            Optional<AssetType> result = service.getReferenceNftSubject(REF_SUBJECT);
            assertThat(result).isEmpty();
        }

        @Test
        void returnsEmptyForNftSubject() {
            String nftSubject = POLICY_ID + NFT_ASSET_NAME;
            Optional<AssetType> result = service.getReferenceNftSubject(nftSubject);
            assertThat(result).isEmpty();
        }

        @Test
        void returnsEmptyForPolicyOnlySubject() {
            Optional<AssetType> result = service.getReferenceNftSubject(POLICY_ID);
            assertThat(result).isEmpty();
        }

        @Test
        void returnsEmptyForLovelace() {
            Optional<AssetType> result = service.getReferenceNftSubject("lovelace");
            assertThat(result).isEmpty();
        }

        @Test
        void roundTripsWithToUnit() {
            Optional<AssetType> refNft = service.getReferenceNftSubject(FT_SUBJECT);

            assertThat(refNft).isPresent();
            assertThat(refNft.get().toUnit()).isEqualTo(POLICY_ID + REF_ASSET_NAME);
        }
    }

    @Nested
    @DisplayName("isReferenceNft")
    class IsReferenceNft {

        @Test
        void returnsTrueForReferenceNftWithQuantityOne() {
            Amt amt = Amt.builder()
                    .unit(POLICY_ID + REF_ASSET_NAME)
                    .quantity(BigInteger.ONE)
                    .build();
            assertThat(service.isReferenceNft(amt)).isTrue();
        }

        @Test
        void returnsFalseForFungibleTokenPrefix() {
            Amt amt = Amt.builder()
                    .unit(FT_SUBJECT)
                    .quantity(BigInteger.ONE)
                    .build();
            assertThat(service.isReferenceNft(amt)).isFalse();
        }

        @Test
        void returnsFalseForQuantityGreaterThanOne() {
            Amt amt = Amt.builder()
                    .unit(POLICY_ID + REF_ASSET_NAME)
                    .quantity(BigInteger.TWO)
                    .build();
            assertThat(service.isReferenceNft(amt)).isFalse();
        }

        @Test
        void returnsFalseForNftPrefix() {
            Amt amt = Amt.builder()
                    .unit(POLICY_ID + NFT_ASSET_NAME)
                    .quantity(BigInteger.ONE)
                    .build();
            assertThat(service.isReferenceNft(amt)).isFalse();
        }
    }

    @Nested
    @DisplayName("containsReferenceNft / extractReferenceNft")
    class ContainsAndExtract {

        @Test
        void containsReturnsTrueWhenReferenceNftPresent() {
            AddressUtxo utxo = utxoWith(POLICY_ID + REF_ASSET_NAME, BigInteger.ONE);
            assertThat(service.containsReferenceNft(utxo)).isTrue();
        }

        @Test
        void containsReturnsFalseWhenNoReferenceNft() {
            AddressUtxo utxo = utxoWith(FT_SUBJECT, BigInteger.ONE);
            assertThat(service.containsReferenceNft(utxo)).isFalse();
        }

        @Test
        void extractReturnsAmtWhenPresent() {
            AddressUtxo utxo = utxoWith(POLICY_ID + REF_ASSET_NAME, BigInteger.ONE);
            Optional<Amt> result = service.extractReferenceNft(utxo);
            assertThat(result).isPresent();
            assertThat(result.get().getUnit()).isEqualTo(POLICY_ID + REF_ASSET_NAME);
        }

        @Test
        void extractReturnsEmptyWhenAbsent() {
            AddressUtxo utxo = utxoWith(FT_SUBJECT, BigInteger.ONE);
            assertThat(service.extractReferenceNft(utxo)).isEmpty();
        }
    }

    @Nested
    @DisplayName("isValidMetadata")
    class IsValidMetadata {

        @Test
        void validWhenNameAndDescriptionPresent() {
            ParsedCip68Datum m = new ParsedCip68Datum(null, "desc", null, "name", null, null, null, null, null, null);
            assertThat(service.isValidMetadata(m)).isTrue();
        }

        @Test
        void invalidWhenNameMissing() {
            ParsedCip68Datum m = new ParsedCip68Datum(null, "desc", null, null, null, null, null, null, null, null);
            assertThat(service.isValidMetadata(m)).isFalse();
        }

        @Test
        void invalidWhenDescriptionMissing() {
            ParsedCip68Datum m = new ParsedCip68Datum(null, null, null, "name", null, null, null, null, null, null);
            assertThat(service.isValidMetadata(m)).isFalse();
        }
    }

    @Nested
    @DisplayName("findSubjects (batch)")
    class FindSubjects {

        private static final String POLICY_A = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        private static final String POLICY_B = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
        private static final String REF_NAME_A = "000643b0aaaa"; // reference NFT asset name
        private static final String REF_NAME_B = "000643b0bbbb";

        @Test
        void returnsEmptyMapForEmptyInput() {
            Map<String, FungibleTokenMetadata> result = service.findSubjects(List.of(), List.of());

            assertThat(result).isEmpty();
            verifyNoInteractions(repository);
        }

        @Test
        void issuesSingleBatchQuery() {
            List<AssetType> keys = List.of(
                    new AssetType(POLICY_A, REF_NAME_A),
                    new AssetType(POLICY_B, REF_NAME_B));
            when(repository.findLatestByConcatenatedKeys(anyCollection()))
                    .thenReturn(List.of());

            service.findSubjects(keys, List.of());

            verify(repository).findLatestByConcatenatedKeys(
                    List.of(POLICY_A + REF_NAME_A, POLICY_B + REF_NAME_B));
        }

        @Test
        void mapsResultsKeyedByRefNftSubject() {
            List<AssetType> keys = List.of(new AssetType(POLICY_A, REF_NAME_A));
            Cip68Metadata row = Cip68Metadata.builder()
                    .policyId(POLICY_A).assetName(REF_NAME_A).slot(100L).label(333)
                    .name("TokenA").description("desc A").ticker("TKA").decimals(6L).version(1L).build();
            when(repository.findLatestByConcatenatedKeys(anyCollection()))
                    .thenReturn(List.of(row));

            Map<String, FungibleTokenMetadata> result = service.findSubjects(keys, List.of());

            assertThat(result).hasSize(1);
            FungibleTokenMetadata md = result.get(POLICY_A + REF_NAME_A);
            assertThat(md).isNotNull();
            assertThat(md.name()).isEqualTo("TokenA");
            assertThat(md.description()).isEqualTo("desc A");
            assertThat(md.ticker()).isEqualTo("TKA");
            assertThat(md.decimals()).isEqualTo(6L);
            assertThat(md.version()).isEqualTo(1L);
        }

        @Test
        void omitsPairsWithNoData() {
            List<AssetType> keys = List.of(
                    new AssetType(POLICY_A, REF_NAME_A),
                    new AssetType(POLICY_B, REF_NAME_B));
            Cip68Metadata rowA = Cip68Metadata.builder()
                    .policyId(POLICY_A).assetName(REF_NAME_A).slot(100L).label(333)
                    .name("A").description("desc A").version(1L).build();
            // Only one row returned from the DB — POLICY_B has no matching row
            when(repository.findLatestByConcatenatedKeys(anyCollection()))
                    .thenReturn(List.of(rowA));

            Map<String, FungibleTokenMetadata> result = service.findSubjects(keys, List.of());

            assertThat(result).hasSize(1);
            assertThat(result).containsOnlyKeys(POLICY_A + REF_NAME_A);
        }

        @Test
        void appliesPropertyFilter() {
            List<AssetType> keys = List.of(new AssetType(POLICY_A, REF_NAME_A));
            Cip68Metadata row = Cip68Metadata.builder()
                    .policyId(POLICY_A).assetName(REF_NAME_A).slot(100L).label(333)
                    .name("TokenA").description("desc A").ticker("TKA").decimals(6L).version(1L)
                    .logo("aGVsbG8=").url("https://example.com").build();
            when(repository.findLatestByConcatenatedKeys(anyCollection()))
                    .thenReturn(List.of(row));

            // Request only name and decimals — everything else should be null
            Map<String, FungibleTokenMetadata> result =
                    service.findSubjects(keys, List.of("name", "decimals"));

            FungibleTokenMetadata md = result.get(POLICY_A + REF_NAME_A);
            assertThat(md.name()).isEqualTo("TokenA");
            assertThat(md.decimals()).isEqualTo(6L);
            assertThat(md.description()).isNull();
            assertThat(md.ticker()).isNull();
            assertThat(md.logo()).isNull();
            assertThat(md.url()).isNull();
            assertThat(md.version()).isNull();
        }

        @Test
        void labelIsNotPassedToRepository() {
            // Regression: a single reference NFT may have rows under multiple labels
            // (the cross-output classifier tags rows by whichever user-token was co-minted).
            // The service contract is "return latest by slot, regardless of label" — i.e.
            // the repository must be called WITHOUT a label argument. Verified observationally
            // on mainnet for cLBC/SHLF, Buckazoids, Burballa/Pernis, Diamond, UTXO, etc.
            List<AssetType> keys = List.of(new AssetType(POLICY_A, REF_NAME_A));
            when(repository.findLatestByConcatenatedKeys(anyCollection()))
                    .thenReturn(List.of());

            service.findSubjects(keys, List.of());

            // No label-aware overload should be invoked.
            verify(repository).findLatestByConcatenatedKeys(anyCollection());
        }
    }

    @Nested
    @DisplayName("findSubject (single)")
    class FindSubject {

        private static final String POLICY = "ccccccccccccccccccccccccccccccccccccccccccccccccccccccc1";
        private static final String REF_NAME = "000643b0cafe";

        @Test
        void returnsLatestRowRegardlessOfLabel() {
            // The mocked repo represents the post-fix contract: it returns the row at the
            // greatest slot for this (policy, asset_name), without any label filter. In real
            // mainnet data, the latest row for a renamed FT (e.g. cLBC → SHLF) lands at
            // label=222 because a 222 token was co-minted in that update transaction — the
            // service must still surface that row's metadata.
            Cip68Metadata latestUnderLabel222 = Cip68Metadata.builder()
                    .policyId(POLICY).assetName(REF_NAME).slot(200L).label(222)
                    .name("SHLF").description("SHLF").ticker("SHLF").decimals(3L).version(1L).build();
            when(repository.findFirstByPolicyIdAndAssetNameOrderBySlotDesc(POLICY, REF_NAME))
                    .thenReturn(Optional.of(latestUnderLabel222));

            Optional<FungibleTokenMetadata> result = service.findSubject(POLICY, REF_NAME, List.of());

            assertThat(result).isPresent();
            assertThat(result.get().name()).isEqualTo("SHLF");
            // The label-aware overload must not be invoked — see findSubject javadoc.
            verify(repository).findFirstByPolicyIdAndAssetNameOrderBySlotDesc(POLICY, REF_NAME);
        }

        @Test
        void returnsEmptyWhenNoRowFound() {
            when(repository.findFirstByPolicyIdAndAssetNameOrderBySlotDesc(POLICY, REF_NAME))
                    .thenReturn(Optional.empty());

            assertThat(service.findSubject(POLICY, REF_NAME, List.of())).isEmpty();
        }
    }

    private static AddressUtxo utxoWith(String unit, BigInteger quantity) {
        return AddressUtxo.builder()
                .amounts(List.of(Amt.builder().unit(unit).quantity(quantity).build()))
                .build();
    }
}

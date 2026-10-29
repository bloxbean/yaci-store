package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.service;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.AssetType;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.Cip68Constants;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.FungibleTokenMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.ParsedCip68Datum;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.model.Cip68Metadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.validation.FieldCategory;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.validation.ValidationError;
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

import static com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.Cip68Constants.LABEL_FT;
import static com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.Cip68Constants.LABEL_NFT;
import static com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.Cip68Constants.LABEL_RFT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
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
        void convertsARichFungibleTokenSubjectToItsReferenceNft() {
            String rftSubject = POLICY_ID + "001bc280" + BASE_NAME;

            Optional<AssetType> result = service.getReferenceNftSubject(rftSubject);

            assertThat(result).isPresent();
            assertThat(result.get().toUnit()).isEqualTo(POLICY_ID + "000643b0" + BASE_NAME);
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
    @DisplayName("extractReferenceNfts")
    class ExtractReferenceNfts {

        private AddressUtxo utxoWith(Amt... amounts) {
            return AddressUtxo.builder().amounts(List.of(amounts)).build();
        }

        private Amt amt(String assetNameHex, long quantity) {
            return Amt.builder().unit(POLICY_ID + assetNameHex).quantity(BigInteger.valueOf(quantity)).build();
        }

        @Test
        void returnsEveryReferenceNftInTheOutputInOrder() {
            Amt first = amt("000643b001", 1);
            Amt second = amt("000643b002", 1);
            Amt third = amt("000643b003", 1);

            assertThat(service.extractReferenceNfts(utxoWith(first, amt("000de14001", 1), second, third)))
                    .containsExactly(first, second, third);
        }

        @Test
        void ignoresAssetsThatAreNotReferenceNfts() {
            // a user token, and a reference-NFT-looking asset with quantity 2
            assertThat(service.extractReferenceNfts(utxoWith(amt("000de14001", 1), amt("000643b001", 2)))).isEmpty();
        }

        @Test
        void returnsOneForTheUsualOutput() {
            Amt only = amt("000643b001", 1);

            assertThat(service.extractReferenceNfts(utxoWith(only))).containsExactly(only);
            assertThat(service.extractReferenceNft(utxoWith(only))).contains(only);
        }
    }

    @Nested
    @DisplayName("findSubjects (batch)")
    class FindSubjects {

        private static final String POLICY_A = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        private static final String POLICY_B = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
        private static final String REF_NAME_A = "000643b0aaaa"; // reference NFT asset name
        private static final String REF_NAME_B = "000643b0bbbb";
        private static final String FT_A = POLICY_A + "0014df10aaaa";
        private static final String RFT_A = POLICY_A + "001bc280aaaa";
        private static final String FT_B = POLICY_B + "0014df10bbbb";

        @Test
        void returnsEmptyMapForEmptyInput() {
            Map<String, FungibleTokenMetadata> result = service.findSubjects(List.of(), List.of());

            assertThat(result).isEmpty();
            verifyNoInteractions(repository);
        }

        @Test
        void skipsSubjectsThatAreNotServedFromCip68WithoutAQuery() {
            assertThat(service.findSubjects(List.of(POLICY_A + REF_NAME_A, POLICY_A + "000de140aaaa"), List.of())).isEmpty();
            verifyNoInteractions(repository);
        }

        @Test
        void issuesSingleBatchQuery() {
            when(repository.findLatestByConcatenatedKeys(anyCollection()))
                    .thenReturn(List.of());

            service.findSubjects(List.of(FT_A, FT_B), List.of());

            verify(repository).findLatestByConcatenatedKeys(
                    List.of(POLICY_A + REF_NAME_A, POLICY_B + REF_NAME_B));
        }

        @Test
        void mapsResultsKeyedBySubjectAskedFor() {
            Cip68Metadata row = Cip68Metadata.builder()
                    .policyId(POLICY_A).assetName(REF_NAME_A).slot(100L).label(333)
                    .name("TokenA").description("desc A").ticker("TKA").decimals(6L).version(1L).build();
            when(repository.findLatestByConcatenatedKeys(anyCollection()))
                    .thenReturn(List.of(row));

            Map<String, FungibleTokenMetadata> result = service.findSubjects(List.of(FT_A), List.of());

            assertThat(result).hasSize(1);
            FungibleTokenMetadata md = result.get(FT_A);
            assertThat(md).isNotNull();
            assertThat(md.name()).isEqualTo("TokenA");
            assertThat(md.description()).isEqualTo("desc A");
            assertThat(md.ticker()).isEqualTo("TKA");
            assertThat(md.decimals()).isEqualTo(6L);
            assertThat(md.version()).isEqualTo(1L);
        }

        @Test
        void omitsPairsWithNoData() {
            Cip68Metadata rowA = Cip68Metadata.builder()
                    .policyId(POLICY_A).assetName(REF_NAME_A).slot(100L).label(333)
                    .name("A").description("desc A").version(1L).build();
            // Only one row returned from the DB — POLICY_B has no matching row
            when(repository.findLatestByConcatenatedKeys(anyCollection()))
                    .thenReturn(List.of(rowA));

            Map<String, FungibleTokenMetadata> result = service.findSubjects(List.of(FT_A, FT_B), List.of());

            assertThat(result).hasSize(1);
            assertThat(result).containsOnlyKeys(FT_A);
        }

        @Test
        void appliesPropertyFilter() {
            Cip68Metadata row = Cip68Metadata.builder()
                    .policyId(POLICY_A).assetName(REF_NAME_A).slot(100L).label(333)
                    .name("TokenA").description("desc A").ticker("TKA").decimals(6L).version(1L)
                    .logo("aGVsbG8=").url("https://example.com").build();
            when(repository.findLatestByConcatenatedKeys(anyCollection()))
                    .thenReturn(List.of(row));

            // Request only name and decimals — everything else should be null
            Map<String, FungibleTokenMetadata> result =
                    service.findSubjects(List.of(FT_A), List.of("name", "decimals"));

            FungibleTokenMetadata md = result.get(FT_A);
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
            when(repository.findLatestByConcatenatedKeys(anyCollection()))
                    .thenReturn(List.of());

            service.findSubjects(List.of(FT_A), List.of());

            // No label-aware overload should be invoked.
            verify(repository).findLatestByConcatenatedKeys(anyCollection());
        }

        @Test
        void servesThe333AndThe444SubjectOfOneReferenceNftEachForItsLabel() {
            // both user tokens share the reference NFT: one query key, two subjects, each checked for its own label
            Cip68Metadata row = Cip68Metadata.builder()
                    .policyId(POLICY_A).assetName(REF_NAME_A).slot(100L).label(333)
                    .name("Both").image("ipfs://QmBoth").version(1L).validationErrors(List.of()).build();
            when(repository.findLatestByConcatenatedKeys(anyCollection()))
                    .thenReturn(List.of(row));

            Map<String, FungibleTokenMetadata> result = service.findSubjects(List.of(FT_A, RFT_A), List.of());

            verify(repository).findLatestByConcatenatedKeys(List.of(POLICY_A + REF_NAME_A));
            assertThat(result).containsOnlyKeys(FT_A, RFT_A);
            assertThat(result.get(FT_A).logo()).isNull();
            assertThat(result.get(FT_A).validationErrors())
                    .extracting(ValidationError::subStandard, ValidationError::fieldName, ValidationError::code)
                    .containsExactly(tuple("333", "description", "missing_value"));
            assertThat(result.get(RFT_A).logo()).isEqualTo("ipfs://QmBoth");
            assertThat(result.get(RFT_A).validationErrors()).isEmpty();
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
            when(repository.findFirstByPolicyIdAndAssetNameOrderBySlotDescTxIndexDesc(POLICY, REF_NAME))
                    .thenReturn(Optional.of(latestUnderLabel222));

            Optional<FungibleTokenMetadata> result = service.findSubject(POLICY, REF_NAME, LABEL_FT, List.of());

            assertThat(result).isPresent();
            assertThat(result.get().name()).isEqualTo("SHLF");
            // The label-aware overload must not be invoked — see findSubject javadoc.
            verify(repository).findFirstByPolicyIdAndAssetNameOrderBySlotDescTxIndexDesc(POLICY, REF_NAME);
        }

        @Test
        void returnsEmptyWhenNoRowFound() {
            when(repository.findFirstByPolicyIdAndAssetNameOrderBySlotDescTxIndexDesc(POLICY, REF_NAME))
                    .thenReturn(Optional.empty());

            assertThat(service.findSubject(POLICY, REF_NAME, LABEL_FT, List.of())).isEmpty();
        }

        @Test
        void returnsATokenThatBreaksCip68WithItsStoredErrorsAndThoseOfItsLabel() {
            ValidationError stored = ValidationError.optional("CIP-68", null, "ticker", "not_utf8",
                    "ticker is not valid UTF-8, stored as hex");
            Cip68Metadata row = Cip68Metadata.builder()
                    .policyId(POLICY).assetName(REF_NAME).slot(200L).label(333)
                    .name("No description").ticker("ff").version(1L).validationErrors(List.of(stored)).build();
            when(repository.findFirstByPolicyIdAndAssetNameOrderBySlotDescTxIndexDesc(POLICY, REF_NAME))
                    .thenReturn(Optional.of(row));

            FungibleTokenMetadata result = service.findSubject(POLICY, REF_NAME, LABEL_FT, List.of()).orElseThrow();

            assertThat(result.name()).isEqualTo("No description");
            assertThat(result.description()).isNull();
            assertThat(result.validationErrors()).first().isEqualTo(stored);
            assertThat(result.validationErrors())
                    .extracting(ValidationError::subStandard, ValidationError::fieldName, ValidationError::fieldCategory)
                    .containsExactly(tuple(null, "ticker", FieldCategory.OPTIONAL),
                            tuple("333", "description", FieldCategory.MANDATORY));
        }

        @Test
        void checksAFungibleTokenWhoseLabelWasGuessedAsAnNftForThe333Label() {
            // a metadata update without the user token: name and image, no description, ticker, logo or decimals, so it
            // was stored as 222. Asked for as a 333 token, the description CIP-68 requires of it is missing
            Cip68Metadata guessedNft = Cip68Metadata.builder()
                    .policyId(POLICY).assetName(REF_NAME).slot(200L).label(LABEL_NFT)
                    .name("Coin").image("ipfs://QmCoin").version(1L).validationErrors(List.of()).build();
            when(repository.findFirstByPolicyIdAndAssetNameOrderBySlotDescTxIndexDesc(POLICY, REF_NAME))
                    .thenReturn(Optional.of(guessedNft));

            FungibleTokenMetadata result = service.findSubject(POLICY, REF_NAME, LABEL_FT, List.of()).orElseThrow();

            assertThat(result.logo()).isNull();
            assertThat(result.validationErrors())
                    .extracting(ValidationError::subStandard, ValidationError::fieldName, ValidationError::fieldCategory,
                            ValidationError::code)
                    .containsExactly(tuple("333", "description", FieldCategory.MANDATORY, "missing_value"));
        }

        @Test
        void aRowWithoutErrorsHasAnEmptyList() {
            Cip68Metadata row = Cip68Metadata.builder()
                    .policyId(POLICY).assetName(REF_NAME).slot(200L).label(333).name("Fine").description("d").version(1L).validationErrors(List.of()).build();
            when(repository.findFirstByPolicyIdAndAssetNameOrderBySlotDescTxIndexDesc(POLICY, REF_NAME))
                    .thenReturn(Optional.of(row));

            assertThat(service.findSubject(POLICY, REF_NAME, LABEL_FT, List.of()).orElseThrow().validationErrors()).isEmpty();
        }

        @Test
        void aRowIndexedBeforeValidationExistedKeepsNullErrors() {
            Cip68Metadata row = Cip68Metadata.builder()
                    .policyId(POLICY).assetName(REF_NAME).slot(200L).label(333).name("Old").description("d").version(1L).build();
            when(repository.findFirstByPolicyIdAndAssetNameOrderBySlotDescTxIndexDesc(POLICY, REF_NAME))
                    .thenReturn(Optional.of(row));

            assertThat(service.findSubject(POLICY, REF_NAME, LABEL_FT, List.of()).orElseThrow().validationErrors()).isNull();
        }

        @Test
        void servesTheImageOfARichFungibleTokenAsItsLogo() {
            Cip68Metadata rft = Cip68Metadata.builder()
                    .policyId(POLICY).assetName(REF_NAME).slot(200L).label(444)
                    .name("Edition").image("ipfs://QmImage").version(1L).build();
            when(repository.findFirstByPolicyIdAndAssetNameOrderBySlotDescTxIndexDesc(POLICY, REF_NAME))
                    .thenReturn(Optional.of(rft));

            assertThat(service.findSubject(POLICY, REF_NAME, LABEL_RFT, List.of()).orElseThrow().logo()).isEqualTo("ipfs://QmImage");
        }

        @Test
        void servesTheImageAsTheLogoOfA444SubjectWhateverLabelTheRowWasStoredWith() {
            // an RFT update without the user token and without decimals is stored as 222; a 333 user token co-minted
            // with a 444 one stores the row as 333
            for (int storedLabel : new int[]{LABEL_NFT, LABEL_FT}) {
                Cip68Metadata row = Cip68Metadata.builder()
                        .policyId(POLICY).assetName(REF_NAME).slot(200L).label(storedLabel)
                        .name("Rich").description("A rich token").image("ipfs://QmRich").version(1L)
                        .validationErrors(List.of()).build();
                when(repository.findFirstByPolicyIdAndAssetNameOrderBySlotDescTxIndexDesc(POLICY, REF_NAME))
                        .thenReturn(Optional.of(row));

                FungibleTokenMetadata result = service.findSubject(POLICY, REF_NAME, LABEL_RFT, List.of()).orElseThrow();

                assertThat(result.logo()).as("stored as %d", storedLabel).isEqualTo("ipfs://QmRich");
                assertThat(result.validationErrors()).as("stored as %d", storedLabel).isEmpty();
            }
        }

        @Test
        void doesNotServeTheImageAsTheLogoOfA333SubjectStoredAsAnRft() {
            Cip68Metadata rft = Cip68Metadata.builder()
                    .policyId(POLICY).assetName(REF_NAME).slot(200L).label(LABEL_RFT)
                    .name("Edition").description("d").image("ipfs://QmImage").version(1L).build();
            when(repository.findFirstByPolicyIdAndAssetNameOrderBySlotDescTxIndexDesc(POLICY, REF_NAME))
                    .thenReturn(Optional.of(rft));

            assertThat(service.findSubject(POLICY, REF_NAME, LABEL_FT, List.of()).orElseThrow().logo()).isNull();
        }

        @Test
        void servesTheImageAsWrittenEvenWhenItsSchemeIsNotAllowed() {
            Cip68Metadata rft = Cip68Metadata.builder()
                    .policyId(POLICY).assetName(REF_NAME).slot(200L).label(444)
                    .name("Edition").image("iagon://x").version(1L).build();
            when(repository.findFirstByPolicyIdAndAssetNameOrderBySlotDescTxIndexDesc(POLICY, REF_NAME))
                    .thenReturn(Optional.of(rft));

            // the validation errors of the row say the scheme is not allowed; the value is not hidden
            assertThat(service.findSubject(POLICY, REF_NAME, LABEL_RFT, List.of()).orElseThrow().logo()).isEqualTo("iagon://x");
        }

        @Test
        void aFungibleTokensOwnLogoIsNotReplacedByItsImage() {
            Cip68Metadata ft = Cip68Metadata.builder()
                    .policyId(POLICY).assetName(REF_NAME).slot(200L).label(333)
                    .name("Coin").logo("ipfs://QmLogo").image("ipfs://QmImage").version(1L).build();
            when(repository.findFirstByPolicyIdAndAssetNameOrderBySlotDescTxIndexDesc(POLICY, REF_NAME))
                    .thenReturn(Optional.of(ft));

            assertThat(service.findSubject(POLICY, REF_NAME, LABEL_FT, List.of()).orElseThrow().logo()).isEqualTo("ipfs://QmLogo");
        }

        @Test
        void aRichFungibleTokensLogoIsNotServedOnlyItsImage() {
            // CIP-68 gives a 444 no logo: one in its datum is not served, also when the image is missing
            Cip68Metadata withImage = Cip68Metadata.builder()
                    .policyId(POLICY).assetName(REF_NAME).slot(200L).label(LABEL_RFT)
                    .name("Edition").logo("ipfs://QmLogo").image("ipfs://QmImage").version(1L).build();
            when(repository.findFirstByPolicyIdAndAssetNameOrderBySlotDescTxIndexDesc(POLICY, REF_NAME))
                    .thenReturn(Optional.of(withImage));
            assertThat(service.findSubject(POLICY, REF_NAME, LABEL_RFT, List.of()).orElseThrow().logo()).isEqualTo("ipfs://QmImage");

            Cip68Metadata withoutImage = Cip68Metadata.builder()
                    .policyId(POLICY).assetName(REF_NAME).slot(200L).label(LABEL_RFT)
                    .name("Edition").logo("ipfs://QmLogo").version(1L).build();
            when(repository.findFirstByPolicyIdAndAssetNameOrderBySlotDescTxIndexDesc(POLICY, REF_NAME))
                    .thenReturn(Optional.of(withoutImage));
            assertThat(service.findSubject(POLICY, REF_NAME, LABEL_RFT, List.of()).orElseThrow().logo()).isNull();
        }
    }

    private static AddressUtxo utxoWith(String unit, BigInteger quantity) {
        return AddressUtxo.builder()
                .amounts(List.of(Amt.builder().unit(unit).quantity(quantity).build()))
                .build();
    }
}

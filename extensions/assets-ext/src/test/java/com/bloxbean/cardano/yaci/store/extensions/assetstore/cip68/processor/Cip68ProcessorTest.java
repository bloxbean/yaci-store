package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.processor;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.invalidtoken.InvalidTokenRecorder;
import org.junit.jupiter.api.BeforeEach;
import java.time.ZoneOffset;
import java.time.Instant;
import java.time.Clock;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.events.internal.PreCommitEvent;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.metrics.Cip68Metrics;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.Cip68Constants;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.model.Cip68Metadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.FungibleTokenMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.ParsedCip68Datum;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.ParseOutcome;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.parser.Cip68DatumParser;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.repository.Cip68MetadataRepository;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.service.Cip68TokenService;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.service.Cip68Validator;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.validation.ValidationError;
import com.bloxbean.cardano.yaci.store.utxo.domain.AddressUtxoEvent;
import com.bloxbean.cardano.yaci.store.utxo.domain.TxInputOutput;
import com.bloxbean.cardano.yaci.store.common.domain.TxInput;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Spy;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Cip68Processor")
class Cip68ProcessorTest {

    private static final String POLICY_ID = "aabbccdd11223344aabbccdd11223344aabbccdd11223344aabbccdd";
    private static final String REF_NFT_ASSET_NAME = "000643b0464c4454";
    private static final String TX_HASH = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890";
    private static final int TX_INDEX = 3;

    @Mock
    private Cip68TokenService cip68TokenService;

    @Mock
    private Cip68DatumParser cip68DatumParser;

    @Mock
    private Cip68MetadataRepository metadataReferenceNftRepository;

    @Spy
    private Cip68Metrics metrics = Cip68Metrics.noop();

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-21T12:00:00Z"), ZoneOffset.UTC);

    private Cip68Processor processor;

    @BeforeEach
    void setUp() {
        processor = new Cip68Processor(cip68TokenService, cip68DatumParser, metadataReferenceNftRepository, metrics,
                InvalidTokenRecorder.noop(), CLOCK);
    }

    @Nested
    @DisplayName("Valid reference NFT UTxOs")
    class ValidReferenceNft {

        @Test
        void savesEntityWithCorrectFields() {
            String datum = "d8799fa34446756e6e";
            ParsedCip68Datum metadata = new ParsedCip68Datum(
                    6L, "A test token", "logo", "TestToken", "TST", "https://test.com", 1L, null, null, null);

            Amt refNftAmt = Amt.builder()
                    .unit(POLICY_ID + REF_NFT_ASSET_NAME)
                    .quantity(BigInteger.ONE)
                    .build();

            AddressUtxo utxo = AddressUtxo.builder()
                    .txHash(TX_HASH)
                    .txIndex(TX_INDEX)
                    .inlineDatum(datum)
                    .amounts(List.of(refNftAmt))
                    .build();

            when(cip68TokenService.extractReferenceNfts(utxo)).thenReturn(List.of(refNftAmt));
            when(cip68DatumParser.parseDetailed(eq(datum), any())).thenReturn(new ParseOutcome.Parsed(metadata));

            processor.processTransaction(buildEvent(100L, utxo));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Iterable<Cip68Metadata>> captor = ArgumentCaptor.forClass(Iterable.class);
            verify(metadataReferenceNftRepository).saveAll(captor.capture());

            Cip68Metadata saved = captor.getValue().iterator().next();
            assertThat(saved.getPolicyId()).isEqualTo(POLICY_ID);
            assertThat(saved.getAssetName()).isEqualTo(REF_NFT_ASSET_NAME);
            assertThat(saved.getSlot()).isEqualTo(100L);
            assertThat(saved.getTxHash()).isEqualTo(TX_HASH);
            assertThat(saved.getTxIndex()).isEqualTo(TX_INDEX);
            assertThat(saved.getLastSyncedAt()).isEqualTo(java.time.LocalDateTime.of(2026, 10, 21, 12, 0));
            assertThat(saved.getName()).isEqualTo("TestToken");
            assertThat(saved.getDescription()).isEqualTo("A test token");
            assertThat(saved.getTicker()).isEqualTo("TST");
            assertThat(saved.getDecimals()).isEqualTo(6);
            assertThat(saved.getDatum()).isEqualTo(datum);
        }
    }

    @Nested
    @DisplayName("Skipped UTxOs")
    class SkippedUtxos {

        @Test
        void skipsWhenNoReferenceNft() {
            AddressUtxo utxo = AddressUtxo.builder()
                    .txHash(TX_HASH)
                    .amounts(List.of(Amt.builder()
                            .unit(POLICY_ID + "deadbeef")
                            .quantity(BigInteger.TEN)
                            .build()))
                    .build();

            when(cip68TokenService.extractReferenceNfts(utxo)).thenReturn(List.of());

            processor.processTransaction(buildEvent(100L, utxo));

            verifyNoInteractions(metadataReferenceNftRepository);
        }

        @Test
        void skipsWhenDatumParsingFails() {
            String datum = "invalid";
            Amt refNftAmt = Amt.builder()
                    .unit(POLICY_ID + REF_NFT_ASSET_NAME)
                    .quantity(BigInteger.ONE)
                    .build();

            AddressUtxo utxo = AddressUtxo.builder()
                    .txHash(TX_HASH)
                    .txIndex(TX_INDEX)
                    .inlineDatum(datum)
                    .amounts(List.of(refNftAmt))
                    .build();

            when(cip68TokenService.extractReferenceNfts(utxo)).thenReturn(List.of(refNftAmt));
            when(cip68DatumParser.parseDetailed(eq(datum), any())).thenReturn(new ParseOutcome.NotCip68());

            processor.processTransaction(buildEvent(100L, utxo));

            verifyNoInteractions(metadataReferenceNftRepository);
        }

        @Test
        void indexesAnInvalidDatumWithItsValidationErrors() {
            String datum = "d8799fa34446756e6e";
            ParsedCip68Datum metadata = new ParsedCip68Datum(
                    null, null, null, null, null, null, null, null, null, null);

            Amt refNftAmt = Amt.builder()
                    .unit(POLICY_ID + REF_NFT_ASSET_NAME)
                    .quantity(BigInteger.ONE)
                    .build();

            AddressUtxo utxo = AddressUtxo.builder()
                    .txHash(TX_HASH)
                    .txIndex(TX_INDEX)
                    .inlineDatum(datum)
                    .amounts(List.of(refNftAmt))
                    .build();

            // minted with its 333 user token: a datum without any of the token fields is not token metadata otherwise
            AddressUtxo userToken = AddressUtxo.builder()
                    .txHash(TX_HASH)
                    .amounts(List.of(Amt.builder().unit(POLICY_ID + "0014df10" + REF_NFT_ASSET_NAME.substring(8))
                            .quantity(BigInteger.ONE).build()))
                    .build();

            when(cip68TokenService.extractReferenceNfts(utxo)).thenReturn(List.of(refNftAmt));
            when(cip68DatumParser.parseDetailed(eq(datum), any())).thenReturn(new ParseOutcome.Parsed(metadata));

            processor.processTransaction(buildEvent(100L, List.of(utxo, userToken)));

            ArgumentCaptor<Iterable<Cip68Metadata>> captor = ArgumentCaptor.forClass(Iterable.class);
            verify(metadataReferenceNftRepository).saveAll(captor.capture());
            assertThat(captor.getValue()).singleElement().satisfies(entity -> {
                assertThat(entity.getLabel()).isEqualTo(Cip68Constants.LABEL_FT);
                // nothing is wrong with the datum itself; what a 333 requires is reported when it is served
                assertThat(entity.getValidationErrors()).isEmpty();
                assertThat(Cip68Validator.validate(entity, Cip68Constants.LABEL_FT))
                        .extracting(e -> e.fieldName() + ":" + e.subStandard() + ":" + e.code())
                        .containsExactly("name:333:missing_value", "description:333:missing_value");
            });
        }
    }

    @Nested
    @DisplayName("Label classification (cross-output detection)")
    class LabelClassification {

        // Common token base name "fdt" (== 464c4454 hex). The reference NFT and the
        // user-token share this base name and only differ by the 4-byte prefix.
        private static final String BASE_NAME_HEX = "464c4454";
        private static final String FT_USER_TOKEN_NAME  = "0014df10" + BASE_NAME_HEX;
        private static final String NFT_USER_TOKEN_NAME = "000de140" + BASE_NAME_HEX;
        private static final String RFT_USER_TOKEN_NAME = "001bc280" + BASE_NAME_HEX;

        @Test
        void labelsAsFtWhenCoMintedWithFungibleUserToken() {
            verifyLabel(FT_USER_TOKEN_NAME, Cip68Constants.LABEL_FT);
        }

        @Test
        void skipsAnNftWhoseTransactionHasOnlyItsNftUserToken() {
            // an NFT's metadata is not served: stored, it would answer for a 333 and a 444 subject never minted
            index(List.of(NFT_USER_TOKEN_NAME));

            verify(metadataReferenceNftRepository, never()).saveAll(any());
            verify(metrics).nftOnly();
        }

        @Test
        void labelsAsNftAnNftOnlyUpdateOfAReferenceNftAlreadyStored() {
            when(metadataReferenceNftRepository.existsByPolicyIdAndAssetName(POLICY_ID, REF_NFT_ASSET_NAME)).thenReturn(true);

            verifyLabel(List.of(NFT_USER_TOKEN_NAME), Cip68Constants.LABEL_NFT);
            verify(metrics, never()).nftOnly();
        }

        @Test
        void storesADatumWhoseTransactionHasTheNftAndTheFungibleToken() {
            // the row is stored with the first label, 222; each token is checked for its own label when it is served
            verifyLabel(List.of(NFT_USER_TOKEN_NAME, FT_USER_TOKEN_NAME), Cip68Constants.LABEL_NFT);
            verify(metrics, never()).nftOnly();
        }

        @Test
        void labelsAsRftWhenCoMintedWithRftUserToken() {
            verifyLabel(RFT_USER_TOKEN_NAME, Cip68Constants.LABEL_RFT);
        }

        @Test
        void fallsBackToFtWhenNoCoMintedUserToken() {
            // Orphan reference NFT: only 000643b0 in the tx, no user-token prefix.
            String datum = "d8799fa34446756e6e";
            ParsedCip68Datum metadata = new ParsedCip68Datum(
                    6L, "Orphan", null, "Orphan", "ORPH", null, 1L, null, null, null);
            Amt refNftAmt = refNftAmount();
            AddressUtxo refNftUtxo = AddressUtxo.builder()
                    .txHash(TX_HASH).inlineDatum(datum).amounts(List.of(refNftAmt)).build();

            when(cip68TokenService.extractReferenceNfts(refNftUtxo)).thenReturn(List.of(refNftAmt));
            when(cip68DatumParser.parseDetailed(eq(datum), any())).thenReturn(new ParseOutcome.Parsed(metadata));

            processor.processTransaction(buildEvent(100L, List.of(refNftUtxo)));

            assertThat(captureSavedLabel()).isEqualTo(Cip68Constants.LABEL_FT);
        }

        // Helper: build a tx with the ref-NFT output + a co-minted user-token output
        // having the given asset name; assert the saved row's label matches expected.
        private void verifyLabel(String coMintedAssetName, int expectedLabel) {
            verifyLabel(List.of(coMintedAssetName), expectedLabel);
        }

        private void verifyLabel(List<String> coMintedAssetNames, int expectedLabel) {
            index(coMintedAssetNames);

            assertThat(captureSavedLabel()).isEqualTo(expectedLabel);
        }

        // Helper: index a tx with the ref-NFT output + one output per co-minted user token
        private void index(List<String> coMintedAssetNames) {
            String datum = "d8799fa34446756e6e";
            ParsedCip68Datum metadata = new ParsedCip68Datum(
                    6L, "Test", null, "Test", "TST", null, 1L, null, null, null);

            Amt refNftAmt = refNftAmount();
            AddressUtxo refNftUtxo = AddressUtxo.builder()
                    .txHash(TX_HASH).inlineDatum(datum).amounts(List.of(refNftAmt)).build();

            // Co-minted user tokens in other outputs of the same tx
            List<AddressUtxo> outputs = new ArrayList<>(List.of(refNftUtxo));
            for (String coMintedAssetName : coMintedAssetNames) {
                Amt userTokenAmt = Amt.builder()
                        .unit(POLICY_ID + coMintedAssetName)
                        .quantity(BigInteger.ONE)
                        .build();
                AddressUtxo userTokenUtxo = AddressUtxo.builder()
                        .txHash(TX_HASH).amounts(List.of(userTokenAmt)).build();
                when(cip68TokenService.extractReferenceNfts(userTokenUtxo)).thenReturn(List.of());
                outputs.add(userTokenUtxo);
            }

            when(cip68TokenService.extractReferenceNfts(refNftUtxo)).thenReturn(List.of(refNftAmt));
            when(cip68DatumParser.parseDetailed(eq(datum), any())).thenReturn(new ParseOutcome.Parsed(metadata));

            // All outputs in the same tx — that's how cross-output detection sees them.
            processor.processTransaction(buildEvent(100L, outputs));
        }

        private Amt refNftAmount() {
            return Amt.builder()
                    .unit(POLICY_ID + REF_NFT_ASSET_NAME)
                    .quantity(BigInteger.ONE)
                    .build();
        }

        private int captureSavedLabel() {
            @SuppressWarnings("unchecked")
            ArgumentCaptor<Iterable<Cip68Metadata>> captor = ArgumentCaptor.forClass(Iterable.class);
            verify(metadataReferenceNftRepository).saveAll(captor.capture());
            return captor.getValue().iterator().next().getLabel();
        }
    }

    /**
     * A datum without a user token in its transaction and with none of the CIP-68 token fields is not token metadata
     * (mainnet: Atrium profiles, empty datums, an {@code insert_fee} datum): it was indexed as a 333 by default.
     */
    @Nested
    @DisplayName("A datum that is not token metadata")
    class NotTokenMetadata {

        private static final String DATUM = "d8799fa0ff";

        private void index(ParsedCip68Datum parsed) {
            Amt refNftAmt = Amt.builder().unit(POLICY_ID + REF_NFT_ASSET_NAME).quantity(BigInteger.ONE).build();
            AddressUtxo utxo = AddressUtxo.builder().txHash(TX_HASH).inlineDatum(DATUM).amounts(List.of(refNftAmt)).build();
            when(cip68TokenService.extractReferenceNfts(utxo)).thenReturn(List.of(refNftAmt));
            when(cip68DatumParser.parseDetailed(eq(DATUM), any())).thenReturn(new ParseOutcome.Parsed(parsed));

            processor.processTransaction(buildEvent(100L, utxo));
        }

        @Test
        void isSkippedAndCountedWithoutAUserTokenAndWithNoneOfTheTokenFields() {
            ParsedCip68Datum dAppState = new ParsedCip68Datum(null, null, null, null, null, null, 1L, null, null,
                    Map.of("additional_properties", Map.of("mainnet-testing", "profile")));

            index(dAppState);

            verify(metadataReferenceNftRepository, never()).saveAll(any());
            verify(metrics).notTokenMetadata();
        }

        @Test
        void isStoredWhenItsReferenceNftIsAlreadyStored() {
            // an owner who clears the metadata must not be served the old one
            when(metadataReferenceNftRepository.existsByPolicyIdAndAssetName(POLICY_ID, REF_NFT_ASSET_NAME)).thenReturn(true);

            index(new ParsedCip68Datum(null, null, null, null, null, null, 1L, null, null, null));

            verify(metadataReferenceNftRepository).saveAll(any());
            verify(metrics, never()).notTokenMetadata();
        }

        @Test
        void isStoredWhenItsOnlyTokenFieldHasTheWrongType() {
            // the field is there, so it is token metadata, broken
            ValidationError wrongName = ValidationError.mandatory("CIP-68", null, "name", "wrong_type", "name is not a byte string, left out");

            index(new ParsedCip68Datum(null, null, null, null, null, null, 1L, null, null, null, List.of(wrongName)));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Iterable<Cip68Metadata>> captor = ArgumentCaptor.forClass(Iterable.class);
            verify(metadataReferenceNftRepository).saveAll(captor.capture());
            assertThat(captor.getValue()).singleElement().satisfies(entity -> assertThat(entity.getValidationErrors())
                    .extracting(e -> e.fieldName() + ":" + e.code()).contains("name:wrong_type"));
        }

        @Test
        void storesAChunkedDescriptionWithoutALabelAndServesItAsOneMandatoryErrorOfA333() {
            ValidationError chunked = ValidationError.optional("CIP-68", null, "description", "chunked_text",
                    "description is a list of byte strings, not one byte string; the chunks are joined");
            AddressUtxo userToken = AddressUtxo.builder().txHash(TX_HASH)
                    .amounts(List.of(Amt.builder().unit(POLICY_ID + "0014df10" + REF_NFT_ASSET_NAME.substring(8))
                            .quantity(BigInteger.ONE).build()))
                    .build();
            Amt refNftAmt = Amt.builder().unit(POLICY_ID + REF_NFT_ASSET_NAME).quantity(BigInteger.ONE).build();
            AddressUtxo utxo = AddressUtxo.builder().txHash(TX_HASH).inlineDatum(DATUM).amounts(List.of(refNftAmt)).build();
            when(cip68TokenService.extractReferenceNfts(utxo)).thenReturn(List.of(refNftAmt));
            when(cip68DatumParser.parseDetailed(eq(DATUM), any())).thenReturn(new ParseOutcome.Parsed(new ParsedCip68Datum(
                    null, "Templar Protocol - Templar lets you borrow", null, "Templar Protocol", null, null, 1L, null, null,
                    null, List.of(chunked))));

            processor.processTransaction(buildEvent(100L, List.of(utxo, userToken)));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Iterable<Cip68Metadata>> captor = ArgumentCaptor.forClass(Iterable.class);
            verify(metadataReferenceNftRepository).saveAll(captor.capture());
            assertThat(captor.getValue()).singleElement().satisfies(entity -> {
                assertThat(entity.getDescription()).isEqualTo("Templar Protocol - Templar lets you borrow");
                assertThat(entity.getValidationErrors()).containsExactly(chunked);
                assertThat(Cip68Validator.validate(entity, Cip68Constants.LABEL_FT))
                        .extracting(e -> e.subStandard() + ":" + e.fieldName() + ":" + e.fieldCategory() + ":" + e.code())
                        .containsExactly("333:description:MANDATORY:chunked_text");
            });
        }
    }

    /**
     * During a parallel sync the blocks of a batch run in partitions at the same time, so a later block can be processed
     * before an earlier one. The module keeps the blocks of the batch and indexes them in chain order at the end of the
     * batch, so that "already stored" sees exactly the earlier blocks.
     */
    @Nested
    @DisplayName("Blocks of a batch indexed in chain order")
    class BatchInChainOrder {

        private static final String MINT_DATUM = "d8799fa1446e616d65444e616d65ff";
        private static final String CLEARED_DATUM = "d8799fa001ff";

        private final List<Cip68Metadata> stored = new ArrayList<>();

        private AddressUtxoEvent block(long block, long slot, String datum) {
            Amt refNftAmt = Amt.builder().unit(POLICY_ID + REF_NFT_ASSET_NAME).quantity(BigInteger.ONE).build();
            AddressUtxo utxo = AddressUtxo.builder().txHash(TX_HASH).txIndex(0).outputIndex(0)
                    .inlineDatum(datum).amounts(List.of(refNftAmt)).build();
            lenient().when(cip68TokenService.extractReferenceNfts(utxo)).thenReturn(List.of(refNftAmt));
            return AddressUtxoEvent.builder()
                    .metadata(EventMetadata.builder().block(block).slot(slot).build())
                    .txInputOutputs(List.of(TxInputOutput.builder().outputs(List.of(utxo)).build()))
                    .build();
        }

        private void storeForReal() {
            // the repository answers "already stored" from what was saved so far, as the database would
            lenient().when(metadataReferenceNftRepository.saveAll(any())).thenAnswer(invocation -> {
                Iterable<Cip68Metadata> rows = invocation.getArgument(0);
                rows.forEach(stored::add);
                return rows;
            });
            lenient().when(metadataReferenceNftRepository.existsByPolicyIdAndAssetName(POLICY_ID, REF_NFT_ASSET_NAME))
                    .thenAnswer(invocation -> !stored.isEmpty());
            lenient().when(cip68DatumParser.parseDetailed(eq(MINT_DATUM), any())).thenReturn(new ParseOutcome.Parsed(
                    new ParsedCip68Datum(null, null, null, "House of Titans #1001", null, null, 1L, null, null, null)));
            lenient().when(cip68DatumParser.parseDetailed(eq(CLEARED_DATUM), any())).thenReturn(new ParseOutcome.Parsed(
                    new ParsedCip68Datum(null, null, null, null, null, null, 1L, null, null, null)));
        }

        @Test
        void aBlockIsNotIndexedBeforeTheEndOfTheBatch() {
            processor.collectForBatch(block(10, 100L, MINT_DATUM));

            verifyNoInteractions(metadataReferenceNftRepository, cip68DatumParser);
        }

        @Test
        void aClearedDatumProcessedBeforeTheMintIsStoredOnceTheBatchIsIndexedInChainOrder() {
            // House of Titans #1001 on mainnet: the datum is cleared 20 blocks after the mint, and the later block's
            // partition ran first
            storeForReal();

            processor.collectForBatch(block(30, 500L, CLEARED_DATUM));
            processor.collectForBatch(block(10, 100L, MINT_DATUM));
            processor.indexBatchInChainOrder(new PreCommitEvent(EventMetadata.builder().block(99).build()));

            assertThat(stored).extracting(Cip68Metadata::getSlot).containsExactly(100L, 500L);
            assertThat(stored.getLast().getName()).isNull();
            verify(metrics, never()).notTokenMetadata();
        }

        @Test
        void theBlocksOfABatchAreIndexedOnce() {
            storeForReal();
            processor.collectForBatch(block(10, 100L, MINT_DATUM));

            processor.indexBatchInChainOrder(new PreCommitEvent(EventMetadata.builder().block(10).build()));
            processor.indexBatchInChainOrder(new PreCommitEvent(EventMetadata.builder().block(11).build()));

            assertThat(stored).hasSize(1);
        }

        @Test
        void aRollbackDropsTheBlocksKeptForTheEndOfTheBatch() {
            processor.collectForBatch(block(10, 100L, MINT_DATUM));

            processor.dropBatch(RollbackEvent.builder().rollbackTo(new Point(50L, "ab".repeat(32))).build());
            processor.indexBatchInChainOrder(new PreCommitEvent(EventMetadata.builder().block(10).build()));

            verifyNoInteractions(metadataReferenceNftRepository, cip68DatumParser);
        }
    }

    /**
     * Until the end of the batch only what indexing reads is kept: the transactions with a reference NFT output, with
     * all their outputs and none of their inputs, so that the memory held grows with the CIP-68 activity of the batch,
     * not with the size of its blocks.
     */
    @Nested
    @DisplayName("What is kept until the end of the batch")
    class KeptForBatch {

        private static final String OTHER_TX_HASH = "1111111111111111111111111111111111111111111111111111111111111111";
        private static final String FT_USER_TOKEN = "0014df10464c4454";

        private AddressUtxo output(String txHash, int outputIndex, String assetName, @Nullable String datum) {
            return AddressUtxo.builder().txHash(txHash).txIndex(0).outputIndex(outputIndex).inlineDatum(datum)
                    .amounts(List.of(Amt.builder().unit(POLICY_ID + assetName).quantity(BigInteger.ONE).build()))
                    .build();
        }

        private AddressUtxo referenceNftOutput(String txHash, String datum) {
            AddressUtxo output = output(txHash, 0, REF_NFT_ASSET_NAME, datum);
            lenient().when(cip68TokenService.extractReferenceNfts(output)).thenReturn(output.getAmounts());
            return output;
        }

        private TxInputOutput tx(String txHash, AddressUtxo... outputs) {
            return TxInputOutput.builder().txHash(txHash)
                    .inputs(List.of(TxInput.builder().txHash(OTHER_TX_HASH).outputIndex(0).build()))
                    .outputs(List.of(outputs)).build();
        }

        private AddressUtxoEvent blockOf(long block, TxInputOutput... txs) {
            return AddressUtxoEvent.builder()
                    .metadata(EventMetadata.builder().block(block).slot(block * 20).build())
                    .txInputOutputs(List.of(txs)).build();
        }

        @Test
        void aBlockWithoutAReferenceNftKeepsNothing() {
            processor.collectForBatch(blockOf(10,
                    tx(TX_HASH, output(TX_HASH, 0, FT_USER_TOKEN, null)),
                    tx(OTHER_TX_HASH, output(OTHER_TX_HASH, 0, "deadbeef", null))));

            assertThat(processor.keptForBatch()).isEmpty();
            processor.indexBatchInChainOrder(new PreCommitEvent(EventMetadata.builder().block(10).build()));
            verifyNoInteractions(metadataReferenceNftRepository, cip68DatumParser);
        }

        @Test
        void keepsOnlyTheTransactionsWithAReferenceNftEachWithAllItsOutputsAndNoInputs() {
            AddressUtxo refNft = referenceNftOutput(TX_HASH, "d8799fa0ff");
            AddressUtxo userToken = output(TX_HASH, 1, FT_USER_TOKEN, null);
            AddressUtxo unrelated = output(OTHER_TX_HASH, 0, "deadbeef", null);

            processor.collectForBatch(blockOf(10, tx(TX_HASH, refNft, userToken), tx(OTHER_TX_HASH, unrelated)));

            assertThat(processor.keptForBatch()).containsOnlyKeys(10L);
            AddressUtxoEvent kept = processor.keptForBatch().get(10L);
            assertThat(kept.getMetadata().getBlock()).isEqualTo(10L);
            assertThat(kept.getMetadata().getSlot()).isEqualTo(200L);
            assertThat(kept.getTxInputOutputs()).singleElement().satisfies(txIo -> {
                assertThat(txIo.getTxHash()).isEqualTo(TX_HASH);
                // the user token is in another output of the transaction: it is kept, so the pairing still sees it
                assertThat(txIo.getOutputs()).containsExactly(refNft, userToken);
                assertThat(txIo.getInputs()).isEmpty();
            });
        }

        @Test
        void theUserTokenOfAKeptTransactionStillPairsWithItsReferenceNft() {
            // name and decimals: an FT datum; the 333 user token in the other output decides the label
            when(cip68DatumParser.parseDetailed(eq("d8799fa0ff"), any())).thenReturn(new ParseOutcome.Parsed(
                    new ParsedCip68Datum(6L, "A coin", null, "Coin", null, null, 1L, null, null, null)));
            AddressUtxo refNft = referenceNftOutput(TX_HASH, "d8799fa0ff");
            AddressUtxo userToken = output(TX_HASH, 1, FT_USER_TOKEN, null);
            when(cip68TokenService.extractReferenceNfts(userToken)).thenReturn(List.of());

            processor.collectForBatch(blockOf(10, tx(TX_HASH, refNft, userToken),
                    tx(OTHER_TX_HASH, output(OTHER_TX_HASH, 0, "deadbeef", null))));
            processor.indexBatchInChainOrder(new PreCommitEvent(EventMetadata.builder().block(10).build()));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Iterable<Cip68Metadata>> captor = ArgumentCaptor.forClass(Iterable.class);
            verify(metadataReferenceNftRepository).saveAll(captor.capture());
            assertThat(captor.getValue()).singleElement().satisfies(row -> {
                assertThat(row.getLabel()).isEqualTo(Cip68Constants.LABEL_FT);
                assertThat(row.getSlot()).isEqualTo(200L);
            });
        }

        @Test
        void aReferenceNftUnitWithAnotherQuantityKeepsItsTransactionForTheExactCheck() {
            // the filter does not check the quantity: processTransaction applies the exact rules to what is kept
            AddressUtxo two = AddressUtxo.builder().txHash(TX_HASH).txIndex(0).outputIndex(0)
                    .amounts(List.of(Amt.builder().unit(POLICY_ID + REF_NFT_ASSET_NAME).quantity(BigInteger.TWO).build()))
                    .build();

            processor.collectForBatch(blockOf(10, tx(TX_HASH, two)));

            assertThat(processor.keptForBatch()).containsOnlyKeys(10L);
        }

        @Test
        void aUnitWithADotBetweenPolicyAndAssetNameIsRecognised() {
            AddressUtxo dotted = AddressUtxo.builder().txHash(TX_HASH).txIndex(0).outputIndex(0)
                    .amounts(List.of(Amt.builder().unit(POLICY_ID + "." + REF_NFT_ASSET_NAME).quantity(BigInteger.ONE).build()))
                    .build();

            processor.collectForBatch(blockOf(10, tx(TX_HASH, dotted)));

            assertThat(processor.keptForBatch()).containsOnlyKeys(10L);
        }

        @Test
        void outputsWithoutAmountsOrUnitsKeepNothingAndDoNotThrow() {
            // read on the block thread, before the per-output error handling: nothing in an output may throw here
            AddressUtxo noAmounts = AddressUtxo.builder().txHash(TX_HASH).txIndex(0).outputIndex(0).build();
            AddressUtxo noUnit = AddressUtxo.builder().txHash(TX_HASH).txIndex(0).outputIndex(1)
                    .amounts(List.of(Amt.builder().quantity(BigInteger.ONE).build())).build();
            AddressUtxo shortUnit = AddressUtxo.builder().txHash(TX_HASH).txIndex(0).outputIndex(2)
                    .amounts(List.of(Amt.builder().unit("000643b0").quantity(BigInteger.ONE).build())).build();
            TxInputOutput noOutputs = TxInputOutput.builder().txHash(OTHER_TX_HASH).build();

            assertThatCode(() -> processor.collectForBatch(blockOf(10, tx(TX_HASH, noAmounts, noUnit, shortUnit), noOutputs)))
                    .doesNotThrowAnyException();
            assertThat(processor.keptForBatch()).isEmpty();
        }

        @Test
        void aBlockCollectedAgainReplacesWhatWasKeptForIt() {
            // a block processed again after a failed batch is not indexed twice
            processor.collectForBatch(blockOf(10, tx(TX_HASH, referenceNftOutput(TX_HASH, "d8799fa0ff"))));
            processor.collectForBatch(blockOf(10, tx(TX_HASH, referenceNftOutput(TX_HASH, "d8799fa0ff"))));

            assertThat(processor.keptForBatch()).containsOnlyKeys(10L);
            assertThat(processor.keptForBatch().get(10L).getTxInputOutputs()).hasSize(1);
        }

        @Test
        void nothingIsKeptOnceTheBatchIsIndexed() {
            processor.collectForBatch(blockOf(10, tx(TX_HASH, referenceNftOutput(TX_HASH, "d8799fa0ff"))));

            processor.indexBatchInChainOrder(new PreCommitEvent(EventMetadata.builder().block(10).build()));

            assertThat(processor.keptForBatch()).isEmpty();
        }
    }

    private AddressUtxoEvent buildEvent(long slot, AddressUtxo utxo) {
        return buildEvent(slot, List.of(utxo));
    }

    private AddressUtxoEvent buildEvent(long slot, List<AddressUtxo> outputs) {
        return AddressUtxoEvent.builder()
                .metadata(EventMetadata.builder().slot(slot).build())
                .txInputOutputs(List.of(TxInputOutput.builder().outputs(outputs).build()))
                .build();
    }

}

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
            endOfBatch();

            verify(metadataReferenceNftRepository, never()).saveAll(any());
            verify(metrics).nftOnly();
        }

        @Test
        void labelsAsNftAnNftOnlyUpdateOfAReferenceNftAlreadyStored() {
            when(metadataReferenceNftRepository.existsEarlierThan(eq(POLICY_ID), eq(REF_NFT_ASSET_NAME), anyLong(), anyInt())).thenReturn(true);

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
            endOfBatch();

            verify(metadataReferenceNftRepository, never()).saveAll(any());
            verify(metrics).notTokenMetadata();
        }

        @Test
        void isStoredWhenItsReferenceNftIsAlreadyStored() {
            // an owner who clears the metadata must not be served the old one
            when(metadataReferenceNftRepository.existsEarlierThan(eq(POLICY_ID), eq(REF_NFT_ASSET_NAME), anyLong(), anyInt())).thenReturn(true);

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
     * before an earlier one has stored its row. A datum stored only if its reference NFT is already indexed is then
     * decided at the end of the batch, in chain order, as a sync of one block at a time would (ADR 0010).
     */
    @Nested
    @DisplayName("Blocks processed out of chain order")
    class OutOfChainOrder {

        private static final String EMPTY_MAP = "d8799fa001ff";
        private static final ParsedCip68Datum CLEARED = new ParsedCip68Datum(null, null, null, null, null, null, 1L, null, null, null);
        private static final String NFT_USER_TOKEN_NAME = "000de140464c4454";

        private AddressUtxo refNftOutput(int txIndex, int outputIndex) {
            Amt refNftAmt = Amt.builder().unit(POLICY_ID + REF_NFT_ASSET_NAME).quantity(BigInteger.ONE).build();
            AddressUtxo utxo = AddressUtxo.builder().txHash(TX_HASH).txIndex(txIndex).outputIndex(outputIndex)
                    .inlineDatum(EMPTY_MAP).amounts(List.of(refNftAmt)).build();
            lenient().when(cip68TokenService.extractReferenceNfts(utxo)).thenReturn(List.of(refNftAmt));
            return utxo;
        }

        private List<Cip68Metadata> savedRows() {
            @SuppressWarnings("unchecked")
            ArgumentCaptor<Iterable<Cip68Metadata>> captor = ArgumentCaptor.forClass(Iterable.class);
            verify(metadataReferenceNftRepository, atLeastOnce()).saveAll(captor.capture());
            List<Cip68Metadata> rows = new ArrayList<>();
            captor.getAllValues().forEach(batch -> batch.forEach(rows::add));
            return rows;
        }

        @Test
        void aClearedDatumIsNotSkippedBeforeTheEndOfTheBatch() {
            // House of Titans #1001 on mainnet: the datum is cleared 20 blocks after the mint, and the block of the mint
            // was in another partition that had not stored its row yet
            when(cip68DatumParser.parseDetailed(eq(EMPTY_MAP), any())).thenReturn(new ParseOutcome.Parsed(CLEARED));
            when(metadataReferenceNftRepository.existsEarlierThan(POLICY_ID, REF_NFT_ASSET_NAME, 102_576_755L, 0))
                    .thenReturn(false, true);

            processor.processTransaction(buildEvent(102_576_755L, refNftOutput(0, 0)));
            verify(metadataReferenceNftRepository, never()).saveAll(any());
            verify(metrics, never()).notTokenMetadata();

            // the partition with the mint has stored its row by the end of the batch
            endOfBatch();

            assertThat(savedRows()).singleElement().satisfies(row -> {
                assertThat(row.getSlot()).isEqualTo(102_576_755L);
                assertThat(row.getName()).isNull();
            });
            verify(metrics, never()).notTokenMetadata();
        }

        @Test
        void aDatumIsSkippedAtTheEndOfTheBatchWhenNoEarlierDatumOfItsReferenceNftIsStored() {
            // the query counts only rows from earlier in the chain: a later block of the batch, stored first, is not one
            when(cip68DatumParser.parseDetailed(eq(EMPTY_MAP), any())).thenReturn(new ParseOutcome.Parsed(CLEARED));

            processor.processTransaction(buildEvent(200L, refNftOutput(0, 0)));
            endOfBatch();

            verify(metadataReferenceNftRepository, never()).saveAll(any());
            verify(metrics).notTokenMetadata();
            verify(metadataReferenceNftRepository, times(2)).existsEarlierThan(POLICY_ID, REF_NFT_ASSET_NAME, 200L, 0);
        }

        @Test
        void theDatumsKeptForTheEndOfTheBatchAreDecidedInChainOrder() {
            // the later block is processed first; the earlier datum is stored because an earlier block of an earlier
            // batch stored its reference NFT, and the later one because the earlier one was just stored
            when(cip68DatumParser.parseDetailed(eq(EMPTY_MAP), any())).thenReturn(new ParseOutcome.Parsed(CLEARED));
            when(metadataReferenceNftRepository.existsEarlierThan(POLICY_ID, REF_NFT_ASSET_NAME, 400L, 0)).thenReturn(false);
            when(metadataReferenceNftRepository.existsEarlierThan(POLICY_ID, REF_NFT_ASSET_NAME, 300L, 1))
                    .thenReturn(false, true);

            processor.processTransaction(buildEvent(400L, refNftOutput(0, 0)));
            processor.processTransaction(buildEvent(300L, refNftOutput(1, 0)));
            endOfBatch();

            assertThat(savedRows()).extracting(Cip68Metadata::getSlot).containsExactly(300L, 400L);
            verify(metrics, never()).notTokenMetadata();
            // the later datum is decided by the earlier one stored in the same pass, not by a query
            verify(metadataReferenceNftRepository, times(1)).existsEarlierThan(POLICY_ID, REF_NFT_ASSET_NAME, 400L, 0);
        }

        @Test
        void anNftsDatumIsStoredAtTheEndOfTheBatchWhenItsReferenceNftWasIndexedEarlier() {
            ParsedCip68Datum nft = new ParsedCip68Datum(null, null, null, "Titan", null, null, 1L, "ipfs://Qm", null, null);
            when(cip68DatumParser.parseDetailed(eq(EMPTY_MAP), any())).thenReturn(new ParseOutcome.Parsed(nft));
            when(metadataReferenceNftRepository.existsEarlierThan(POLICY_ID, REF_NFT_ASSET_NAME, 500L, 0))
                    .thenReturn(false, true);
            AddressUtxo userToken = AddressUtxo.builder().txHash(TX_HASH).txIndex(0).outputIndex(1)
                    .amounts(List.of(Amt.builder().unit(POLICY_ID + NFT_USER_TOKEN_NAME).quantity(BigInteger.ONE).build()))
                    .build();
            when(cip68TokenService.extractReferenceNfts(userToken)).thenReturn(List.of());

            processor.processTransaction(buildEvent(500L, List.of(refNftOutput(0, 0), userToken)));
            endOfBatch();

            assertThat(savedRows()).singleElement().extracting(Cip68Metadata::getLabel).isEqualTo(Cip68Constants.LABEL_NFT);
            verify(metrics, never()).nftOnly();
        }

        @Test
        void aRollbackDropsTheDatumsKeptForTheEndOfTheBatch() {
            when(cip68DatumParser.parseDetailed(eq(EMPTY_MAP), any())).thenReturn(new ParseOutcome.Parsed(CLEARED));

            processor.processTransaction(buildEvent(600L, refNftOutput(0, 0)));
            processor.dropPendingDatums(RollbackEvent.builder().rollbackTo(new Point(500L, "ab".repeat(32))).build());
            endOfBatch();

            verify(metadataReferenceNftRepository, times(1)).existsEarlierThan(POLICY_ID, REF_NFT_ASSET_NAME, 600L, 0);
            verify(metrics, never()).notTokenMetadata();
        }
    }

    private void endOfBatch() {
        processor.indexPendingDatums(new PreCommitEvent(EventMetadata.builder().build()));
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

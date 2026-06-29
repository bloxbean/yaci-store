package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.processor;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.Cip68Constants;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.model.Cip68Metadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.FungibleTokenMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.ParsedCip68Datum;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.parser.Cip68DatumParser;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.repository.Cip68MetadataRepository;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.service.Cip68TokenService;
import com.bloxbean.cardano.yaci.store.utxo.domain.AddressUtxoEvent;
import com.bloxbean.cardano.yaci.store.utxo.domain.TxInputOutput;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.util.List;
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

    @InjectMocks
    private Cip68Processor processor;

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

            when(cip68TokenService.extractReferenceNft(utxo)).thenReturn(Optional.of(refNftAmt));
            when(cip68DatumParser.parse(datum)).thenReturn(Optional.of(metadata));
            when(cip68TokenService.isValidMetadata(metadata)).thenReturn(true);

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

            when(cip68TokenService.extractReferenceNft(utxo)).thenReturn(Optional.empty());

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

            when(cip68TokenService.extractReferenceNft(utxo)).thenReturn(Optional.of(refNftAmt));
            when(cip68DatumParser.parse(datum)).thenReturn(Optional.empty());

            processor.processTransaction(buildEvent(100L, utxo));

            verifyNoInteractions(metadataReferenceNftRepository);
        }

        @Test
        void skipsWhenMetadataInvalid() {
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

            when(cip68TokenService.extractReferenceNft(utxo)).thenReturn(Optional.of(refNftAmt));
            when(cip68DatumParser.parse(datum)).thenReturn(Optional.of(metadata));
            when(cip68TokenService.isValidMetadata(metadata)).thenReturn(false);

            processor.processTransaction(buildEvent(100L, utxo));

            verifyNoInteractions(metadataReferenceNftRepository);
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
        void labelsAsNftWhenCoMintedWithNftUserToken() {
            verifyLabel(NFT_USER_TOKEN_NAME, Cip68Constants.LABEL_NFT);
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

            when(cip68TokenService.extractReferenceNft(refNftUtxo)).thenReturn(Optional.of(refNftAmt));
            when(cip68DatumParser.parse(datum)).thenReturn(Optional.of(metadata));
            when(cip68TokenService.isValidMetadata(metadata)).thenReturn(true);

            processor.processTransaction(buildEvent(100L, List.of(refNftUtxo)));

            assertThat(captureSavedLabel()).isEqualTo(Cip68Constants.LABEL_FT);
        }

        // Helper: build a tx with the ref-NFT output + a co-minted user-token output
        // having the given asset name; assert the saved row's label matches expected.
        private void verifyLabel(String coMintedAssetName, int expectedLabel) {
            String datum = "d8799fa34446756e6e";
            ParsedCip68Datum metadata = new ParsedCip68Datum(
                    6L, "Test", null, "Test", "TST", null, 1L, null, null, null);

            Amt refNftAmt = refNftAmount();
            AddressUtxo refNftUtxo = AddressUtxo.builder()
                    .txHash(TX_HASH).inlineDatum(datum).amounts(List.of(refNftAmt)).build();

            // Co-minted user token in another output of the same tx
            Amt userTokenAmt = Amt.builder()
                    .unit(POLICY_ID + coMintedAssetName)
                    .quantity(BigInteger.ONE)
                    .build();
            AddressUtxo userTokenUtxo = AddressUtxo.builder()
                    .txHash(TX_HASH).amounts(List.of(userTokenAmt)).build();

            when(cip68TokenService.extractReferenceNft(refNftUtxo)).thenReturn(Optional.of(refNftAmt));
            when(cip68TokenService.extractReferenceNft(userTokenUtxo)).thenReturn(Optional.empty());
            when(cip68DatumParser.parse(datum)).thenReturn(Optional.of(metadata));
            when(cip68TokenService.isValidMetadata(metadata)).thenReturn(true);

            // Both outputs in the same tx — that's how cross-output detection sees them.
            processor.processTransaction(buildEvent(100L, List.of(refNftUtxo, userTokenUtxo)));

            assertThat(captureSavedLabel()).isEqualTo(expectedLabel);
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

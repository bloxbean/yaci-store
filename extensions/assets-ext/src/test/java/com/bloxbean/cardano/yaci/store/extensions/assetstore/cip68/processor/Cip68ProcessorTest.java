package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.processor;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.model.MetadataReferenceNft;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.FungibleTokenMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.parser.Cip68DatumParser;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.repository.MetadataReferenceNftRepository;
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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.time.Clock;
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

    @Mock
    private Cip68TokenService cip68TokenService;

    @Mock
    private Cip68DatumParser cip68DatumParser;

    @Mock
    private MetadataReferenceNftRepository metadataReferenceNftRepository;

    @Spy
    private Clock clock = Clock.systemDefaultZone();

    @InjectMocks
    private Cip68Processor processor;

    @Nested
    @DisplayName("Valid reference NFT UTxOs")
    class ValidReferenceNft {

        @Test
        void savesEntityWithCorrectFields() {
            String datum = "d8799fa34446756e6e";
            FungibleTokenMetadata metadata = new FungibleTokenMetadata(
                    6L, "A test token", "logo", "TestToken", "TST", "https://test.com", 1L);

            Amt refNftAmt = Amt.builder()
                    .unit(POLICY_ID + REF_NFT_ASSET_NAME)
                    .quantity(BigInteger.ONE)
                    .build();

            AddressUtxo utxo = AddressUtxo.builder()
                    .txHash(TX_HASH)
                    .inlineDatum(datum)
                    .amounts(List.of(refNftAmt))
                    .build();

            when(cip68TokenService.extractReferenceNft(utxo)).thenReturn(Optional.of(refNftAmt));
            when(cip68DatumParser.parse(datum)).thenReturn(Optional.of(metadata));
            when(cip68TokenService.isValidMetadata(metadata)).thenReturn(true);

            processor.processTransaction(buildEvent(100L, utxo));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Iterable<MetadataReferenceNft>> captor = ArgumentCaptor.forClass(Iterable.class);
            verify(metadataReferenceNftRepository).saveAll(captor.capture());

            MetadataReferenceNft saved = captor.getValue().iterator().next();
            assertThat(saved.getPolicyId()).isEqualTo(POLICY_ID);
            assertThat(saved.getAssetName()).isEqualTo(REF_NFT_ASSET_NAME);
            assertThat(saved.getSlot()).isEqualTo(100L);
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
            FungibleTokenMetadata metadata = new FungibleTokenMetadata(
                    null, null, null, null, null, null, null);

            Amt refNftAmt = Amt.builder()
                    .unit(POLICY_ID + REF_NFT_ASSET_NAME)
                    .quantity(BigInteger.ONE)
                    .build();

            AddressUtxo utxo = AddressUtxo.builder()
                    .txHash(TX_HASH)
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

    private AddressUtxoEvent buildEvent(long slot, AddressUtxo utxo) {
        return AddressUtxoEvent.builder()
                .metadata(EventMetadata.builder().slot(slot).build())
                .txInputOutputs(List.of(TxInputOutput.builder().outputs(List.of(utxo)).build()))
                .build();
    }

}

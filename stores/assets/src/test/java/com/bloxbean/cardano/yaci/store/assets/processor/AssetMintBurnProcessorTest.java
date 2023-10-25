package com.bloxbean.cardano.yaci.store.assets.processor;

import com.bloxbean.cardano.yaci.core.model.Amount;
import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.assets.domain.MintType;
import com.bloxbean.cardano.yaci.store.assets.domain.TxAsset;
import com.bloxbean.cardano.yaci.store.assets.storage.AssetStorage;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.MintBurnEvent;
import com.bloxbean.cardano.yaci.store.events.domain.TxMintBurn;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssetMintBurnProcessorTest {

    @Mock
    private AssetStorage assetStorage;
    @Mock
    private ApplicationEventPublisher publisher;
    @InjectMocks
    private AssetMintBurnProcessor assetMintBurnProcessor;

    @Captor
    ArgumentCaptor<List<TxAsset>> argCaptor;

    @Test
    void givenMintBurnEvent_shouldHandleMintBurnEventAndSaveTxAssets() {
        final MintBurnEvent mintBurnEvent = MintBurnEvent.builder()
                .metadata(EventMetadata.builder()
                        .block(204450)
                        .blockTime(1666901639L)
                        .slot(11218439L)
                        .era(Era.Babbage)
                        .build())
                .txMintBurns(txMintBurns())
                .build();

        assetMintBurnProcessor.handleAssetMintBurn(mintBurnEvent);

        verify(assetStorage, times(1)).saveAll(argCaptor.capture());
        List<TxAsset> txAssets = argCaptor.getValue();

        assertThat(txAssets).hasSize(2);

        assertThat(txAssets.get(0).getTxHash()).isEqualTo("fd960815810b788da1f1d8719e3fdb47c5e4a82b9527f9c337a49512d255d545");
        assertThat(txAssets.get(0).getAssetName()).isEqualTo("ATADAcoin");
        assertThat(txAssets.get(0).getFingerprint()).isEqualTo("asset1ee0u29k4xwauf0r7w8g30klgraxw0y4rz2t7xs");
        assertThat(txAssets.get(0).getSlot()).isEqualTo(11218439L);
        assertThat(txAssets.get(0).getBlockNumber()).isEqualTo(204450);
        assertThat(txAssets.get(0).getBlockTime()).isEqualTo(1666901639L);
        assertThat(txAssets.get(0).getMintType()).isEqualTo(MintType.MINT);
        assertThat(txAssets.get(0).getQuantity()).isEqualTo(BigInteger.ONE);
        assertThat(txAssets.get(0).getPolicy()).isEqualTo("34250edd1e9836f5378702fbf9416b709bc140e04f668cc355208518");
        assertThat(txAssets.get(0).getUnit()).isEqualTo("34250edd1e9836f5378702fbf9416b709bc140e04f668cc3552085184154414441636f696e");

        assertThat(txAssets.get(1).getTxHash()).isEqualTo("2f6ca7f9e7d31e60c8445b7ac793d8fe20506a471784cec2c1ee1627acf256f7");
        assertThat(txAssets.get(1).getAssetName()).isEqualTo("dtsNFT");
        assertThat(txAssets.get(1).getFingerprint()).isEqualTo("asset179arv9zfxjw8qsuw7jx20qnyf3ltgy7czsqhg4");
        assertThat(txAssets.get(1).getSlot()).isEqualTo(11218439L);
        assertThat(txAssets.get(1).getBlockNumber()).isEqualTo(204450);
        assertThat(txAssets.get(1).getBlockTime()).isEqualTo(1666901639L);
        assertThat(txAssets.get(1).getMintType()).isEqualTo(MintType.BURN);
        assertThat(txAssets.get(1).getQuantity()).isEqualTo(BigInteger.ZERO);
        assertThat(txAssets.get(1).getPolicy()).isEqualTo("3bc07438218b280dc651d825bd0e3276fc01e9faf73f0bda13c08327");
        assertThat(txAssets.get(1).getUnit()).isEqualTo("3bc07438218b280dc651d825bd0e3276fc01e9faf73f0bda13c083276474734e4654");
    }

    @Test
    void givenMintBurnEvent_DoNotHandleTxMintBurnThatHaveAmountsIsNull() {
        List<TxMintBurn> txMintBurns = txMintBurns();
        txMintBurns.set(1, TxMintBurn.builder()
                .txHash("2f6ca7f9e7d31e60c8445b7ac793d8fe20506a471784cec2c1ee1627acf256f7")
                .amounts(null)
                .build());
        final MintBurnEvent mintBurnEvent = MintBurnEvent.builder()
                .metadata(EventMetadata.builder()
                        .block(204450)
                        .blockTime(1666901639L)
                        .slot(11218439L)
                        .era(Era.Babbage)
                        .build())
                .txMintBurns(txMintBurns)
                .build();

        assetMintBurnProcessor.handleAssetMintBurn(mintBurnEvent);

        verify(assetStorage, times(1)).saveAll(argCaptor.capture());
        List<TxAsset> txAssets = argCaptor.getValue();
        assertThat(txAssets).hasSize(1);

        assertThat(txAssets.get(0).getTxHash()).isEqualTo("fd960815810b788da1f1d8719e3fdb47c5e4a82b9527f9c337a49512d255d545");
        assertThat(txAssets.get(0).getAssetName()).isEqualTo("ATADAcoin");
        assertThat(txAssets.get(0).getFingerprint()).isEqualTo("asset1ee0u29k4xwauf0r7w8g30klgraxw0y4rz2t7xs");
        assertThat(txAssets.get(0).getSlot()).isEqualTo(11218439L);
        assertThat(txAssets.get(0).getBlockNumber()).isEqualTo(204450);
        assertThat(txAssets.get(0).getBlockTime()).isEqualTo(1666901639L);
        assertThat(txAssets.get(0).getMintType()).isEqualTo(MintType.MINT);
        assertThat(txAssets.get(0).getQuantity()).isEqualTo(BigInteger.valueOf(1));
        assertThat(txAssets.get(0).getPolicy()).isEqualTo("34250edd1e9836f5378702fbf9416b709bc140e04f668cc355208518");
        assertThat(txAssets.get(0).getUnit()).isEqualTo("34250edd1e9836f5378702fbf9416b709bc140e04f668cc3552085184154414441636f696e");
    }

    @Test
    void givenMintBurnEvent_WhenTxMintBurnsIsEmpty_DoNotSaveAnyTxAssets() {
        final MintBurnEvent mintBurnEvent = MintBurnEvent.builder()
                .metadata(EventMetadata.builder()
                        .block(204450)
                        .blockTime(1666901639L)
                        .slot(11218439L)
                        .era(Era.Babbage)
                        .build())
                .txMintBurns(List.of())
                .build();
        assetMintBurnProcessor.handleAssetMintBurn(mintBurnEvent);
        verify(assetStorage, never()).saveAll(anyList());
    }

    private List<TxMintBurn> txMintBurns() {
        List<TxMintBurn> result = new ArrayList<>();

        TxMintBurn txMintBurn1 = TxMintBurn.builder()
                .txHash("fd960815810b788da1f1d8719e3fdb47c5e4a82b9527f9c337a49512d255d545")
                .amounts(List.of(
                        Amount.builder()
                                .unit("34250edd1e9836f5378702fbf9416b709bc140e04f668cc3552085184154414441636f696e")
                                .policyId("34250edd1e9836f5378702fbf9416b709bc140e04f668cc355208518")
                                .assetName("ATADAcoin")
                                .quantity(BigInteger.valueOf(1))
                                .build()))
                .build();

        TxMintBurn txMintBurn2 = TxMintBurn.builder()
                .txHash("2f6ca7f9e7d31e60c8445b7ac793d8fe20506a471784cec2c1ee1627acf256f7")
                .amounts(List.of(
                        Amount.builder()
                                .unit("3bc07438218b280dc651d825bd0e3276fc01e9faf73f0bda13c083276474734e4654")
                                .policyId("3bc07438218b280dc651d825bd0e3276fc01e9faf73f0bda13c08327")
                                .assetName("dtsNFT")
                                .quantity(BigInteger.valueOf(0))
                                .build()))
                .build();

        result.add(txMintBurn1);
        result.add(txMintBurn2);

        return result;
    }
}

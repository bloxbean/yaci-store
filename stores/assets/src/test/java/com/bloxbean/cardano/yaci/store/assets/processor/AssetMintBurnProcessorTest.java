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
        assertThat(txAssets.get(0).getTxIndex()).isEqualTo(0);
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
        assertThat(txAssets.get(1).getTxIndex()).isEqualTo(3);
        assertThat(txAssets.get(1).getBlockNumber()).isEqualTo(204450);
        assertThat(txAssets.get(1).getBlockTime()).isEqualTo(1666901639L);
        assertThat(txAssets.get(1).getMintType()).isEqualTo(MintType.BURN);
        assertThat(txAssets.get(1).getQuantity()).isEqualTo(BigInteger.valueOf(-1));
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
    void givenMintBurnEvent_withZeroQuantityEntries_doNotSaveThem() {
        String policyId = "ebe691a343665487dbca5d1853cfb798b68e5ab3e1f699240e2cf999";
        Amount zero = Amount.builder()
                .unit(policyId + "68764d494e")
                .policyId(policyId)
                .assetName("hvMIN")
                .assetNameBytes("hvMIN".getBytes())
                .quantity(BigInteger.ZERO)
                .build();
        Amount burn = Amount.builder()
                .unit(policyId + "6876414441")
                .policyId(policyId)
                .assetName("hvADA")
                .assetNameBytes("hvADA".getBytes())
                .quantity(BigInteger.valueOf(-130))
                .build();
        final MintBurnEvent mintBurnEvent = MintBurnEvent.builder()
                .metadata(EventMetadata.builder()
                        .block(1315584)
                        .blockTime(1693193954L)
                        .slot(37510754L)
                        .era(Era.Babbage)
                        .build())
                .txMintBurns(List.of(
                        TxMintBurn.builder()
                                .txHash("6749a57f25d85fe52f84d8cc83a7bc381c1d26614f967af9588c0cdf00ab5c36")
                                .txIndex(0)
                                .amounts(List.of(zero, burn))
                                .build(),
                        TxMintBurn.builder()
                                .txHash("14c430c046280178c274a02dd611a157f1bfc6512644285e9eb803c013695104")
                                .txIndex(1)
                                .amounts(List.of(zero))
                                .build()))
                .build();

        assetMintBurnProcessor.handleAssetMintBurn(mintBurnEvent);

        verify(assetStorage, times(1)).saveAll(argCaptor.capture());
        assertThat(argCaptor.getValue()).singleElement().satisfies(txAsset -> {
            assertThat(txAsset.getUnit()).isEqualTo(policyId + "6876414441");
            assertThat(txAsset.getQuantity()).isEqualTo(BigInteger.valueOf(-130));
            assertThat(txAsset.getMintType()).isEqualTo(MintType.BURN);
        });
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
                .txIndex(0)
                .amounts(List.of(
                        Amount.builder()
                                .unit("34250edd1e9836f5378702fbf9416b709bc140e04f668cc3552085184154414441636f696e")
                                .policyId("34250edd1e9836f5378702fbf9416b709bc140e04f668cc355208518")
                                .assetName("ATADAcoin")
                                .assetNameBytes("ATADAcoin".getBytes())
                                .quantity(BigInteger.valueOf(1))
                                .build()))
                .build();

        TxMintBurn txMintBurn2 = TxMintBurn.builder()
                .txHash("2f6ca7f9e7d31e60c8445b7ac793d8fe20506a471784cec2c1ee1627acf256f7")
                .txIndex(3)
                .amounts(List.of(
                        Amount.builder()
                                .unit("3bc07438218b280dc651d825bd0e3276fc01e9faf73f0bda13c083276474734e4654")
                                .policyId("3bc07438218b280dc651d825bd0e3276fc01e9faf73f0bda13c08327")
                                .assetName("dtsNFT")
                                .assetNameBytes("dtsNFT".getBytes())
                                .quantity(BigInteger.valueOf(-1))
                                .build()))
                .build();

        result.add(txMintBurn1);
        result.add(txMintBurn2);

        return result;
    }
}

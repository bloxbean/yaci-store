package com.bloxbean.cardano.yaci.store.epochnonce.service;

import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.blocks.domain.Block;
import com.bloxbean.cardano.yaci.store.blocks.storage.BlockStorageReader;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.epoch.domain.EpochParam;
import com.bloxbean.cardano.yaci.store.epoch.storage.EpochParamStorage;
import com.bloxbean.cardano.yaci.store.epochnonce.domain.EpochNonce;
import com.bloxbean.cardano.yaci.store.epochnonce.storage.EpochNonceStorage;
import com.bloxbean.cardano.yaci.store.epochnonce.util.EpochNonceConfig;
import com.bloxbean.cardano.yaci.store.epochnonce.util.NonceUtil;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ResourceLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EpochNonceServiceTest {
    private static final String CANDIDATE_NONCE = "1111111111111111111111111111111111111111111111111111111111111111";
    private static final String EVOLVING_NONCE = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final String LAB_NONCE = "3333333333333333333333333333333333333333333333333333333333333333";
    private static final String PREVIOUS_LAB_NONCE = "2222222222222222222222222222222222222222222222222222222222222222";
    private static final String EXTRA_ENTROPY = "d982e06fd33e7440b43cefad529b7ecafbaa255e38178ad4189a37e4ce9bf1fa";

    @Mock
    private BlockStorageReader blockStorageReader;

    @Mock
    private EpochNonceStorage epochNonceStorage;

    @Mock
    private EpochNonceConfig epochNonceConfig;

    @Mock
    private StoreProperties storeProperties;

    @Mock
    private ResourceLoader resourceLoader;

    @Mock
    private EpochParamStorage epochParamStorage;

    @Captor
    private ArgumentCaptor<EpochNonce> epochNonceCaptor;

    private EpochNonceService epochNonceService;

    @BeforeEach
    void setUp() {
        epochNonceService = new EpochNonceService(
                blockStorageReader,
                epochNonceStorage,
                epochNonceConfig,
                storeProperties,
                resourceLoader,
                Optional.of(epochParamStorage));
    }

    @Test
    void computeEpochNonce_tPraosNonNeutralExtraEntropy_combinesAfterCandidateAndPreviousLabNonce() {
        int newEpoch = 259;
        int completedEpoch = 258;
        stubPreviousNonceState(completedEpoch);
        when(epochParamStorage.getProtocolParams(newEpoch)).thenReturn(epochParam(newEpoch, "1," + EXTRA_ENTROPY));

        epochNonceService.computeEpochNonce(newEpoch, completedEpoch, metadata(Era.Shelley));

        verify(epochNonceStorage).save(epochNonceCaptor.capture());
        String expected = encode(NonceUtil.combineNonces(
                NonceUtil.combineNonces(decode(CANDIDATE_NONCE), decode(PREVIOUS_LAB_NONCE)),
                decode(EXTRA_ENTROPY)));
        assertThat(epochNonceCaptor.getValue().getNonce()).isEqualTo(expected);
    }

    @Test
    void computeEpochNonce_tPraosNeutralExtraEntropy_matchesExistingFormula() {
        int newEpoch = 260;
        int completedEpoch = 259;
        stubPreviousNonceState(completedEpoch);
        when(epochParamStorage.getProtocolParams(newEpoch)).thenReturn(epochParam(newEpoch, "0,"));

        epochNonceService.computeEpochNonce(newEpoch, completedEpoch, metadata(Era.Shelley));

        verify(epochNonceStorage).save(epochNonceCaptor.capture());
        assertThat(epochNonceCaptor.getValue().getNonce()).isEqualTo(existingFormulaNonce());
    }

    @Test
    void computeEpochNonce_tPraosMissingEpochParam_matchesExistingFormula() {
        int newEpoch = 258;
        int completedEpoch = 257;
        stubPreviousNonceState(completedEpoch);
        when(epochParamStorage.getProtocolParams(newEpoch)).thenReturn(Optional.empty());

        epochNonceService.computeEpochNonce(newEpoch, completedEpoch, metadata(Era.Shelley));

        verify(epochNonceStorage).save(epochNonceCaptor.capture());
        assertThat(epochNonceCaptor.getValue().getNonce()).isEqualTo(existingFormulaNonce());
    }

    @Test
    void computeEpochNonce_tPraosMissingEpochParamStorage_matchesExistingFormula() {
        int newEpoch = 258;
        int completedEpoch = 257;
        epochNonceService = new EpochNonceService(
                blockStorageReader,
                epochNonceStorage,
                epochNonceConfig,
                storeProperties,
                resourceLoader,
                Optional.empty());
        stubPreviousNonceState(completedEpoch);

        epochNonceService.computeEpochNonce(newEpoch, completedEpoch, metadata(Era.Shelley));

        verify(epochNonceStorage).save(epochNonceCaptor.capture());
        assertThat(epochNonceCaptor.getValue().getNonce()).isEqualTo(existingFormulaNonce());
        verify(epochParamStorage, never()).getProtocolParams(any(Integer.class));
    }

    @Test
    void computeEpochNonce_babbageEra_doesNotReadOrApplyExtraEntropy() {
        int newEpoch = 366;
        int completedEpoch = 365;
        stubPreviousNonceState(completedEpoch);

        epochNonceService.computeEpochNonce(newEpoch, completedEpoch, metadata(Era.Babbage));

        verify(epochNonceStorage).save(epochNonceCaptor.capture());
        assertThat(epochNonceCaptor.getValue().getNonce()).isEqualTo(existingFormulaNonce());
        verify(epochParamStorage, never()).getProtocolParams(any(Integer.class));
    }

    @Test
    void computeEpochNonce_babbageNewEpochAfterTPraosCompletedEpoch_doesNotReadOrApplyExtraEntropy() {
        int newEpoch = 366;
        int completedEpoch = 365;
        stubPreviousNonceState(completedEpoch, List.of(
                Block.builder()
                        .slot(1L)
                        .epochSlot(1)
                        .era(Era.Alonzo.getValue())
                        .build()));

        epochNonceService.computeEpochNonce(newEpoch, completedEpoch, metadata(Era.Babbage), Era.Babbage);

        verify(epochNonceStorage).save(epochNonceCaptor.capture());
        assertThat(epochNonceCaptor.getValue().getNonce()).isEqualTo(existingFormulaNonce());
        verify(epochParamStorage, never()).getProtocolParams(any(Integer.class));
    }

    private void stubPreviousNonceState(int completedEpoch) {
        stubPreviousNonceState(completedEpoch, List.of());
    }

    private void stubPreviousNonceState(int completedEpoch, List<Block> blocks) {
        EpochNonce prevState = EpochNonce.builder()
                .epoch(completedEpoch)
                .nonce(CANDIDATE_NONCE)
                .evolvingNonce(EVOLVING_NONCE)
                .candidateNonce(CANDIDATE_NONCE)
                .labNonce(LAB_NONCE)
                .lastEpochBlockNonce(PREVIOUS_LAB_NONCE)
                .build();

        when(epochNonceStorage.findByEpoch(completedEpoch)).thenReturn(Optional.of(prevState));
        when(blockStorageReader.findBlocksByEpoch(completedEpoch)).thenReturn(new ArrayList<>(blocks));
        when(epochNonceConfig.getStabilityWindow(any())).thenReturn(129600L);
        when(epochNonceConfig.getEpochLength()).thenReturn(432000L);
    }

    private Optional<EpochParam> epochParam(int epoch, String extraEntropy) {
        return Optional.of(EpochParam.builder()
                .epoch(epoch)
                .params(ProtocolParams.builder()
                        .extraEntropy(extraEntropy)
                        .build())
                .build());
    }

    private EventMetadata metadata(Era era) {
        return EventMetadata.builder()
                .era(era)
                .slot(1000)
                .block(100)
                .blockTime(1000000)
                .build();
    }

    private String existingFormulaNonce() {
        return encode(NonceUtil.combineNonces(decode(CANDIDATE_NONCE), decode(PREVIOUS_LAB_NONCE)));
    }

    private byte[] decode(String hex) {
        return HexUtil.decodeHexString(hex);
    }

    private String encode(byte[] bytes) {
        return HexUtil.encodeHexString(bytes);
    }
}

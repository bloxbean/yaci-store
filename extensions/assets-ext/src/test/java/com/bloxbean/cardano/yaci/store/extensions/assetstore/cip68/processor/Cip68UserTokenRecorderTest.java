package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.processor;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.model.Cip68UserToken;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.repository.Cip68UserTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Cip68UserTokenRecorder")
class Cip68UserTokenRecorderTest {

    private static final String POLICY = "aa".repeat(28);
    private static final String FT = POLICY + "0014df10" + "6162";
    private static final String RFT = POLICY + "001bc280" + "6162";
    private static final String TX = "bb".repeat(32);

    private Cip68UserTokenRepository repository;
    private Cip68UserTokenRecorder recorder;

    @BeforeEach
    void setUp() {
        repository = mock(Cip68UserTokenRepository.class);
        recorder = new Cip68UserTokenRecorder(repository);
    }

    @Test
    @DisplayName("a token that is not recorded yet is saved with its slot and transaction")
    void savesNewTokens() {
        Map<String, String> seen = new LinkedHashMap<>();
        seen.put(FT, TX);
        seen.put(RFT, TX);

        recorder.record(seen, 123L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Cip68UserToken>> saved = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(saved.capture());
        assertThat(saved.getValue()).extracting(Cip68UserToken::getUnit).containsExactly(FT, RFT);
        assertThat(saved.getValue()).allSatisfy(t -> {
            assertThat(t.getSlot()).isEqualTo(123L);
            assertThat(t.getTxHash()).isEqualTo(TX);
        });
    }

    @Test
    @DisplayName("a token that is already recorded is left alone, so a replayed block adds nothing")
    void knownTokensAreNotSavedAgain() {
        when(repository.findAllById(any())).thenReturn(List.of(Cip68UserToken.builder().unit(FT).slot(1L).txHash(TX).build()));

        recorder.record(Map.of(FT, TX), 123L);

        verify(repository, never()).saveAll(any());
    }

    @Test
    @DisplayName("only the tokens that are new are saved when a block shows new and known ones")
    void savesOnlyTheNewOnes() {
        when(repository.findAllById(any())).thenReturn(List.of(Cip68UserToken.builder().unit(FT).slot(1L).txHash(TX).build()));
        Map<String, String> seen = new LinkedHashMap<>();
        seen.put(FT, TX);
        seen.put(RFT, TX);

        recorder.record(seen, 123L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Cip68UserToken>> saved = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(saved.capture());
        assertThat(saved.getValue()).extracting(Cip68UserToken::getUnit).containsExactly(RFT);
    }

    @Test
    @DisplayName("nothing is read or written for a block without user tokens")
    void emptyBlockDoesNothing() {
        recorder.record(Map.of(), 123L);

        verify(repository, never()).findAllById(any());
        verify(repository, never()).saveAll(any());
    }

    @Test
    @DisplayName("a failing repository is a warning, never an exception: it must not stop the sync")
    void failureIsContained() {
        when(repository.findAllById(any())).thenThrow(new IllegalStateException("db down"));

        assertThatCode(() -> recorder.record(Map.of(FT, TX), 123L)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("the no-op recorder records nothing and does not throw")
    void noop() {
        assertThatCode(() -> Cip68UserTokenRecorder.noop().record(Map.of(FT, TX), 1L)).doesNotThrowAnyException();
    }
}

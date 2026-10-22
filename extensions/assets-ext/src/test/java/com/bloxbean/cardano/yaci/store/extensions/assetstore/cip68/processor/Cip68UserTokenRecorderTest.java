package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.processor;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.model.Cip68UserToken;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.repository.Cip68UserTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
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
    private Cip68UserTokenWriter writer;
    private Cip68UserTokenRecorder recorder;

    @BeforeEach
    void setUp() {
        repository = mock(Cip68UserTokenRepository.class);
        writer = mock(Cip68UserTokenWriter.class);
        recorder = new Cip68UserTokenRecorder(repository, writer);
    }

    private static Cip68UserToken recorded(String unit, long slot) {
        return Cip68UserToken.builder().unit(unit).slot(slot).txHash(TX).build();
    }

    @Test
    @DisplayName("a token that is not recorded yet is written with its slot and transaction, in a fixed order")
    void writesNewTokensInOrder() {
        Map<String, String> seen = new LinkedHashMap<>();
        seen.put(RFT, TX);   // 001bc280 sorts after 0014df10
        seen.put(FT, TX);

        recorder.recordSeen(seen, 123L);

        InOrder order = inOrder(writer);
        order.verify(writer).upsertFirstSeen(FT, TX, 123L);
        order.verify(writer).upsertFirstSeen(RFT, TX, 123L);
    }

    @Test
    @DisplayName("a token recorded at this slot or earlier is left alone, so a replayed block adds nothing")
    void knownTokensAreNotWrittenAgain() {
        when(repository.findAllById(any())).thenReturn(List.of(recorded(FT, 123L), recorded(RFT, 50L)));
        Map<String, String> seen = new LinkedHashMap<>();
        seen.put(FT, TX);
        seen.put(RFT, TX);

        recorder.recordSeen(seen, 123L);

        verify(writer, never()).upsertFirstSeen(any(), any(), anyLong());
    }

    @Test
    @DisplayName("a token recorded at a later slot (a later block committed first) is written again, so that the row keeps the earlier slot")
    void laterRecordedSlotIsLowered() {
        when(repository.findAllById(any())).thenReturn(List.of(recorded(FT, 200L)));

        recorder.recordSeen(Map.of(FT, TX), 100L);

        verify(writer).upsertFirstSeen(FT, TX, 100L);
    }

    @Test
    @DisplayName("only the tokens that are new are written when a block shows new and known ones")
    void writesOnlyTheNewOnes() {
        when(repository.findAllById(any())).thenReturn(List.of(recorded(FT, 1L)));
        Map<String, String> seen = new LinkedHashMap<>();
        seen.put(FT, TX);
        seen.put(RFT, TX);

        recorder.recordSeen(seen, 123L);

        verify(writer, never()).upsertFirstSeen(eq(FT), any(), anyLong());
        verify(writer).upsertFirstSeen(RFT, TX, 123L);
    }

    @Test
    @DisplayName("nothing is read or written for a block without user tokens")
    void emptyBlockDoesNothing() {
        recorder.recordSeen(Map.of(), 123L);

        verify(repository, never()).findAllById(any());
        verify(writer, never()).upsertFirstSeen(any(), any(), anyLong());
    }

    @Test
    @DisplayName("a failing repository or writer is a warning, never an exception")
    void failureIsContained() {
        when(repository.findAllById(any())).thenThrow(new IllegalStateException("db down"));
        assertThatCode(() -> recorder.recordSeen(Map.of(FT, TX), 123L)).doesNotThrowAnyException();

        doReturn(List.of()).when(repository).findAllById(any());
        doThrow(new IllegalStateException("db down")).when(writer).upsertFirstSeen(any(), any(), anyLong());
        assertThatCode(() -> recorder.recordSeen(Map.of(FT, TX), 123L)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("the no-op recorder records nothing and does not throw")
    void noop() {
        assertThatCode(() -> Cip68UserTokenRecorder.noop().recordSeen(Map.of(FT, TX), 1L)).doesNotThrowAnyException();
    }
}

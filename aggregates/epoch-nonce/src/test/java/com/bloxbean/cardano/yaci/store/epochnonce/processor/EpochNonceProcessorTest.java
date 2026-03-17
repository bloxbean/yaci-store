package com.bloxbean.cardano.yaci.store.epochnonce.processor;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.store.epochnonce.service.EpochNonceService;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.events.internal.EpochTransitionCommitEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EpochNonceProcessorTest {

    @Mock
    private EpochNonceService epochNonceService;

    @InjectMocks
    private EpochNonceProcessor epochNonceProcessor;

    @Test
    void handleEpochTransition_byronEra_shouldSkip() {
        EpochTransitionCommitEvent event = EpochTransitionCommitEvent.builder()
                .epoch(5)
                .previousEpoch(4)
                .era(Era.Byron)
                .previousEra(Era.Byron)
                .metadata(EventMetadata.builder().slot(100).block(5).blockTime(1000).build())
                .build();

        epochNonceProcessor.handleEpochTransition(event);

        verifyNoInteractions(epochNonceService);
    }

    @Test
    void handleEpochTransition_nullPreviousEpochWithEpochGreaterThanZero_shouldSkip() {
        EpochTransitionCommitEvent event = EpochTransitionCommitEvent.builder()
                .epoch(210)
                .previousEpoch(null)
                .era(Era.Babbage)
                .metadata(EventMetadata.builder().slot(100).block(5).blockTime(1000).build())
                .build();

        epochNonceProcessor.handleEpochTransition(event);

        verifyNoInteractions(epochNonceService);
    }

    @Test
    void handleEpochTransition_shelleyEra_shouldCallService() {
        EventMetadata metadata = EventMetadata.builder()
                .slot(4492800)
                .block(4490560)
                .blockTime(1596491091)
                .build();

        EpochTransitionCommitEvent event = EpochTransitionCommitEvent.builder()
                .epoch(211)
                .previousEpoch(210)
                .era(Era.Shelley)
                .previousEra(Era.Shelley)
                .metadata(metadata)
                .build();

        epochNonceProcessor.handleEpochTransition(event);

        verify(epochNonceService).computeEpochNonce(211, 210, metadata);
    }

    @Test
    void handleEpochTransition_babbageEra_shouldCallService() {
        EventMetadata metadata = EventMetadata.builder()
                .slot(72316800)
                .block(7791600)
                .blockTime(1664409600)
                .build();

        EpochTransitionCommitEvent event = EpochTransitionCommitEvent.builder()
                .epoch(366)
                .previousEpoch(365)
                .era(Era.Babbage)
                .previousEra(Era.Babbage)
                .metadata(metadata)
                .build();

        epochNonceProcessor.handleEpochTransition(event);

        verify(epochNonceService).computeEpochNonce(366, 365, metadata);
    }

    @Test
    void handleEpochTransition_nullPreviousEpochWithEpochZero_shouldCallService() {
        // For custom networks where epoch 0 can be directly in Shelley era
        EventMetadata metadata = EventMetadata.builder()
                .slot(0)
                .block(0)
                .blockTime(1000)
                .build();

        EpochTransitionCommitEvent event = EpochTransitionCommitEvent.builder()
                .epoch(0)
                .previousEpoch(null)
                .era(Era.Shelley)
                .metadata(metadata)
                .build();

        epochNonceProcessor.handleEpochTransition(event);

        verify(epochNonceService).computeEpochNonce(0, null, metadata);
    }

    @Test
    void handleRollback_shouldDelegateToService() {
        RollbackEvent rollbackEvent = RollbackEvent.builder()
                .rollbackTo(new Point(44635389, "4bbe984de25c79052af653c2424122a7b324b27143886849028789a597ce4ae6"))
                .currentPoint(new Point(44635470, "d4a58c8cce51691d680c68480013059c6e0713ebf4a42aed4f2a6714a30e256c"))
                .build();

        epochNonceProcessor.handleRollback(rollbackEvent);

        verify(epochNonceService).rollback(44635389);
    }
}

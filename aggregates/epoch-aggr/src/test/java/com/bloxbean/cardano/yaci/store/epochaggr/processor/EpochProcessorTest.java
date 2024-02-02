package com.bloxbean.cardano.yaci.store.epochaggr.processor;

import com.bloxbean.cardano.yaci.store.epochaggr.service.EpochService;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.internal.CommitEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EpochProcessorTest {
    @Mock
    private EpochService epochService;

    @InjectMocks
    private EpochProcessor epochProcessor;

    @Test
    void testScheduleEpochDataAggregation() {
        epochProcessor.scheduleEpochDataAggregation();
        Mockito.verify(epochService).aggregateData();
    }

    @Test
    void givenBlockHeaderEvent_whenCurrentEpochIsPreEpoch_shouldReturn() {
        ReflectionTestUtils.setField(epochProcessor, "prevEpoch", 100);
        epochProcessor.handleCommitEvent(CommitEvent.builder()
                .metadata(EventMetadata.builder()
                        .epochNumber(100)
                        .build())
                .build());

        Mockito.verify(epochService, Mockito.never()).aggregateData();
    }

    @Test
    void givenBlockHeaderEvent_ShouldAggregateEpochData() {
        epochProcessor.handleCommitEvent(CommitEvent.builder()
                .metadata(EventMetadata.builder()
                        .epochNumber(100)
                        .build())
                .build());

        Mockito.verify(epochService).aggregateData();
    }
}

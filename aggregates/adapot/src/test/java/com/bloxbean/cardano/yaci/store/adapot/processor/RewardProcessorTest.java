package com.bloxbean.cardano.yaci.store.adapot.processor;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.store.adapot.job.storage.AdaPotJobStorage;
import com.bloxbean.cardano.yaci.store.adapot.snapshot.InstantRewardSnapshotService;
import com.bloxbean.cardano.yaci.store.adapot.storage.RewardStorage;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RewardProcessorTest {

    @Mock
    private RewardStorage rewardStorage;
    @Mock
    private InstantRewardSnapshotService instantRewardSnapshotService;
    @Mock
    private AdaPotJobStorage rewardCalcJobStorage;
    @Mock
    private EraService eraService;

    @InjectMocks
    private RewardProcessor rewardProcessor;

    @Test
    void handleRollbackEvent_boundsRewardDeleteByTheEpochOfTheRollbackSlot() {
        when(eraService.getFirstNonByronEpoch()).thenReturn(Optional.of(4));
        when(eraService.getEpochNo(Era.Shelley, 119084867L)).thenReturn(320);

        rewardProcessor.handleRollbackEvent(rollbackTo(119084867L));

        verify(rewardStorage).deleteRewardsBySlotGreaterThan(119084867L, 320);
    }

    @Test
    void handleRollbackEvent_usesNoEpochBoundBeforeShelley() {
        when(eraService.getFirstNonByronEpoch()).thenReturn(Optional.empty());

        rewardProcessor.handleRollbackEvent(rollbackTo(1000L));

        verify(eraService, never()).getEpochNo(Era.Shelley, 1000L);
        verify(rewardStorage).deleteRewardsBySlotGreaterThan(1000L, 0);
    }

    private static RollbackEvent rollbackTo(long slot) {
        return RollbackEvent.builder()
                .rollbackTo(new Point(slot, "hash"))
                .build();
    }
}

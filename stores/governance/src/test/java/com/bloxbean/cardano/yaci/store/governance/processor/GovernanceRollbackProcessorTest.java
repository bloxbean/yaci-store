package com.bloxbean.cardano.yaci.store.governance.processor;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.governance.storage.GovActionProposalStorage;
import com.bloxbean.cardano.yaci.store.governance.storage.VotingProcedureStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GovernanceRollbackProcessorTest {

    @Mock
    private VotingProcedureStorage votingProcedureStorage;
    @Mock
    private GovActionProposalStorage govActionProposalStorage;

    @InjectMocks
    private GovernanceRollbackProcessor governanceRollbackProcessor;

    @Test
    void testHandleRollbackEvent() {
        RollbackEvent rollbackEvent = RollbackEvent.builder()
                .currentPoint(new Point(78650212, "07de1aad598dd499d44be8bd8c28d5cadf4f47fbfb6b2144ccfb376c996ffcec"))
                .rollbackTo(new Point(78650198, "ff86fbda2fc051e9337c9bcd7d58d82948000cb7ed5658f306046ad154f8389e"))
                .build();

        governanceRollbackProcessor.handleRollbackEvent(rollbackEvent);

        verify(govActionProposalStorage, times(1)).deleteBySlotGreaterThan(78650198);
        verify(votingProcedureStorage, times(1)).deleteBySlotGreaterThan(78650198);
    }
}

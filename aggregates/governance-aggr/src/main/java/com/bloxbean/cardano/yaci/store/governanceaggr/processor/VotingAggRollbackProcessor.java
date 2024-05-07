package com.bloxbean.cardano.yaci.store.governanceaggr.processor;

import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.governanceaggr.service.CommitteeVoteService;
import com.bloxbean.cardano.yaci.store.governanceaggr.service.LatestVotingProcedureService;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.CommitteeVoteStorage;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.LatestVotingProcedureStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class VotingAggRollbackProcessor {
    private final LatestVotingProcedureStorage latestVotingProcedureStorage;
    private final LatestVotingProcedureService latestVotingProcedureService;
    private final CommitteeVoteStorage committeeVoteStorage;
    private final CommitteeVoteService committeeVoteService;

    @EventListener
    @Transactional
    public void handleRollbackEvent(RollbackEvent rollbackEvent) {
        int count = latestVotingProcedureStorage.deleteBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());
        log.info("Rollback -- {} latest voting procedure records", count);
        count = committeeVoteStorage.deleteBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());
        log.info("Rollback -- {} committee_vote records", count);

        latestVotingProcedureService.syncUpLatestVotingProcedure();
        log.info("Re Calculate Committee Vote after Rollback");
        committeeVoteService.calculateAndSaveCommitteeVoteBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());
    }
}

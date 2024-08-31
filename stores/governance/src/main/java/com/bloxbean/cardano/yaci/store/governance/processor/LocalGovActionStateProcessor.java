package com.bloxbean.cardano.yaci.store.governance.processor;

import com.bloxbean.cardano.yaci.store.events.BlockEvent;
import com.bloxbean.cardano.yaci.store.events.EpochChangeEvent;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.governance.service.LocalGovStateService;
import com.bloxbean.cardano.yaci.store.governance.storage.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnBean(LocalGovStateService.class)
public class LocalGovActionStateProcessor {
    private final LocalGovStateService localGovStateService;
    private final LocalGovActionProposalStatusStorage localGovActionProposalStatusStorage;
    private final LocalConstitutionStorage localConstitutionStorage;
    private final LocalCommitteeMemberStorage localCommitteeMemberStorage;
    private final LocalCommitteeStorage localCommitteeStorage;
    private final LocalTreasuryWithdrawalStorage localTreasuryWithdrawalStorage;

    private boolean syncMode = false;

    @EventListener
    public void blockEvent(BlockEvent blockEvent) {
        syncMode = blockEvent.getMetadata().isSyncMode();
    }

    @EventListener
    @Transactional
    public void handleEpochChangeEvent(EpochChangeEvent epochChangeEvent) {
        syncMode = epochChangeEvent.getEventMetadata().isSyncMode();
        if (!syncMode)
            return;

        log.info("Epoch change event received. Fetching and updating local gov state");
        localGovStateService.fetchAndSetGovState();
    }

    @EventListener
    @Transactional
    public void handleRollbackEvent(RollbackEvent rollbackEvent) {
        long slot = rollbackEvent.getRollbackTo().getSlot();

        int count = localConstitutionStorage.deleteBySlotGreaterThan(slot);
        log.info("Rollback -- {} local_constitution records", count);
        count = localCommitteeMemberStorage.deleteBySlotGreaterThan(slot);
        log.info("Rollback -- {} local_committee records", count);
        count = localCommitteeStorage.deleteBySlotGreaterThan(slot);
        log.info("Rollback -- {} local_committee_member records", count);
        count = localTreasuryWithdrawalStorage.deleteBySlotGreaterThan(slot);
        log.info("Rollback -- {} local_treasury_withdrawal records", count);
        count = localGovActionProposalStatusStorage.deleteBySlotGreaterThan(slot);
        log.info("Rollback -- {} local_gov_action_proposal_status records", count);

        if (count > 0) {
            log.info("Fetch gov state after rollback event...");
            localGovStateService.fetchAndSetGovState();
        }
    }

    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.MINUTES)
    public void scheduleFetchAndSetGovState() {
        if (!syncMode) {
            return;
        }

        log.info("Fetching gov state by scheduler....");
        localGovStateService.fetchAndSetGovState();
    }
}

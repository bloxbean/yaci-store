package com.bloxbean.cardano.yaci.store.governance.processor;

import com.bloxbean.cardano.yaci.store.events.EpochChangeEvent;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.governance.service.LocalGovStateService;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.repository.*;
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
    private final LocalGovActionProposalStatusRepository localGovActionProposalStatusRepository;
    private final LocalConstitutionRepository localConstitutionRepository;
    private final LocalCommitteeMemberRepository localCommitteeMemberRepository;
    private final LocalCommitteeRepository localCommitteeRepository;
    private final LocalTreasuryWithdrawalRepository localTreasuryWithdrawalRepository;

    @EventListener
    @Transactional
    public void handleEpochChangeEvent(EpochChangeEvent epochChangeEvent) {
        if (!epochChangeEvent.getEventMetadata().isSyncMode())
            return;

        log.info("Epoch change event received. Fetching and updating local gov state");
        localGovStateService.fetchAndSetGovState();
    }

    @EventListener
    @Transactional
    public void handleRollbackEvent(RollbackEvent rollbackEvent) {
        long slot = rollbackEvent.getRollbackTo().getSlot();
        int count = localGovActionProposalStatusRepository.deleteBySlotGreaterThan(slot);
        localConstitutionRepository.deleteBySlotGreaterThan(slot);
        localCommitteeRepository.deleteBySlotGreaterThan(slot);
        localCommitteeMemberRepository.deleteBySlotGreaterThan(slot);
        localTreasuryWithdrawalRepository.deleteBySlotGreaterThan(slot);

        if (count > 0) {
            log.info("Fetch gov state after rollback event...");
            localGovStateService.fetchAndSetGovState();
        }
    }

    @Scheduled(fixedRate = 2, timeUnit = TimeUnit.MINUTES)
    public void scheduleFetchAndSetCurrentProtocolParams() {
        log.info("Fetching gov state by scheduler....");
        localGovStateService.fetchAndSetGovState();
    }
}

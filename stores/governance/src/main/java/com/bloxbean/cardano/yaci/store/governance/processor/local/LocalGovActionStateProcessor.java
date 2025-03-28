package com.bloxbean.cardano.yaci.store.governance.processor.local;

import com.bloxbean.cardano.yaci.store.common.aspect.EnableIf;
import com.bloxbean.cardano.yaci.core.protocol.localstate.api.Era;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.events.BlockEvent;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.governance.service.LocalGovStateService;
import com.bloxbean.cardano.yaci.store.governance.storage.local.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

import static com.bloxbean.cardano.yaci.store.governance.GovernanceStoreConfiguration.STORE_GOVERNANCE_ENABLED;

@Component
@Slf4j
@ConditionalOnBean(LocalGovStateService.class)
@EnableIf(STORE_GOVERNANCE_ENABLED)
public class LocalGovActionStateProcessor {
    private final LocalGovStateService localGovStateService;
    private final LocalGovActionProposalStatusStorage localGovActionProposalStatusStorage;
    private final LocalConstitutionStorage localConstitutionStorage;
    private final LocalCommitteeMemberStorage localCommitteeMemberStorage;
    private final LocalCommitteeStorage localCommitteeStorage;
    private final LocalTreasuryWithdrawalStorage localTreasuryWithdrawalStorage;
    private final StoreProperties storeProperties;
    private boolean syncMode = false;

    public LocalGovActionStateProcessor(LocalGovStateService localGovStateService, LocalGovActionProposalStatusStorage localGovActionProposalStatusStorage, LocalConstitutionStorage localConstitutionStorage, LocalCommitteeMemberStorage localCommitteeMemberStorage, LocalCommitteeStorage localCommitteeStorage, LocalTreasuryWithdrawalStorage localTreasuryWithdrawalStorage, StoreProperties storeProperties) {
        this.localGovStateService = localGovStateService;
        this.localGovActionProposalStatusStorage = localGovActionProposalStatusStorage;
        this.localConstitutionStorage = localConstitutionStorage;
        this.localCommitteeMemberStorage = localCommitteeMemberStorage;
        this.localCommitteeStorage = localCommitteeStorage;
        this.localTreasuryWithdrawalStorage = localTreasuryWithdrawalStorage;
        this.storeProperties = storeProperties;

        if (!storeProperties.isSyncAutoStart()) {
            log.info("Auto sync is disabled. updating local governance state will be ignored");
        }
    }

    @EventListener
    public void blockEvent(BlockEvent blockEvent) {
        syncMode = blockEvent.getMetadata().isSyncMode();
    }

    @EventListener
    @Transactional
    public void handleRollbackEvent(RollbackEvent rollbackEvent) {
        if (!syncMode) {
            return;
        }

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

    @Scheduled(fixedRateString = "${store.governance.n2c-gov-state-fetching-interval-in-minutes:5}", timeUnit = TimeUnit.MINUTES)
    public void scheduleFetchAndSetGovState() {
        if (!syncMode || !storeProperties.isSyncAutoStart()){
            return;
        }

        if (localGovStateService.getEra() != null && localGovStateService.getEra().getValue() >= Era.Conway.getValue()) {
            try {
                log.info("Fetching gov state by scheduler....");
                localGovStateService.fetchAndSetGovState();
            } catch (Exception e) {
                log.error("Fetching gov state failed", e);
            }
        }
    }
}

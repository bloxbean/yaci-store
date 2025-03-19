package com.bloxbean.cardano.yaci.store.governance.processor;

import com.bloxbean.cardano.yaci.store.common.aspect.EnableIf;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.governance.storage.GovActionProposalStorage;
import com.bloxbean.cardano.yaci.store.governance.storage.VotingProcedureStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.bloxbean.cardano.yaci.store.governance.GovernanceStoreConfiguration.STORE_GOVERNANCE_ENABLED;

@Component
@RequiredArgsConstructor
@EnableIf(STORE_GOVERNANCE_ENABLED)
@Slf4j
public class GovernanceRollbackProcessor {

    private final VotingProcedureStorage votingProcedureStorage;
    private final GovActionProposalStorage govActionProposalStorage;

    @EventListener
    @Transactional
    public void handleRollbackEvent(RollbackEvent rollbackEvent) {
        int count = govActionProposalStorage.deleteBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());
        log.info("Rollback -- {} GovActionProposal records", count);

        count = votingProcedureStorage.deleteBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());
        log.info("Rollback -- {} VotingProcedure records", count);
    }

}

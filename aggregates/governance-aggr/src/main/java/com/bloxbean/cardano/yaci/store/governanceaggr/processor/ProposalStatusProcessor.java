package com.bloxbean.cardano.yaci.store.governanceaggr.processor;

import com.bloxbean.cardano.yaci.store.events.domain.StakeSnapshotTakenEvent;
import com.bloxbean.cardano.yaci.store.governance.storage.GovActionProposalStorage;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.GovActionStatus;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.GovActionProposalStatus;
import com.bloxbean.cardano.yaci.store.governanceaggr.service.DRepDistService;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.GovActionProposalStatusStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProposalStatusProcessor {
    private final GovActionProposalStatusStorage govActionProposalStatusStorage;
    private final GovActionProposalStorage govActionProposalStorage;
    private final DRepDistService dRepDistService;

    @EventListener
    @Transactional
    public void handleProposalStatus(StakeSnapshotTakenEvent stakeSnapshotTakenEvent) {
        // get new active proposals in the recent epoch and save them into governance_action_proposal_status table
        // take dRep stake distribution snapshot
        // use gov rule and update proposal status

        int epoch = stakeSnapshotTakenEvent.getEpoch();
        // get new active proposals in the recent epoch and save them into governance_action_proposal_status table
        var newProposals = govActionProposalStorage.findByEpoch(stakeSnapshotTakenEvent.getEpoch());

        govActionProposalStatusStorage.saveAll(newProposals.stream().map(govActionProposal ->
                GovActionProposalStatus.builder()
                        .govActionTxHash(govActionProposal.getTxHash())
                        .govActionIndex((int) govActionProposal.getIndex())
                        .epoch(govActionProposal.getEpoch())
                        .slot(govActionProposal.getSlot())
                        .status(GovActionStatus.ACTIVE)
                        .build()
        ).toList());
        dRepDistService.takeStakeSnapshot(stakeSnapshotTakenEvent.getEpoch());
    }
}


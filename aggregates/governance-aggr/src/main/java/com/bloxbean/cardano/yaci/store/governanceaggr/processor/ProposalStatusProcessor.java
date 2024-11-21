package com.bloxbean.cardano.yaci.store.governanceaggr.processor;

import com.bloxbean.cardano.yaci.store.events.domain.StakeSnapshotTakenEvent;
import com.bloxbean.cardano.yaci.store.governance.domain.GovActionProposal;
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

import java.util.List;

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
        int epoch = stakeSnapshotTakenEvent.getEpoch();
        // get new active proposals in the recent epoch
        List<GovActionProposal> newGovActionProposals = govActionProposalStorage.findByEpoch(epoch);

        // save into gov_action_proposal_status
        if (!newGovActionProposals.isEmpty()) {
            govActionProposalStatusStorage.saveAll(newGovActionProposals.stream()
                    .map(govActionProposal -> GovActionProposalStatus.builder()
                            .govActionTxHash(govActionProposal.getTxHash())
                            .govActionIndex((int) govActionProposal.getIndex())
                            .status(GovActionStatus.ACTIVE)
                            .epoch(govActionProposal.getEpoch())
                            .slot(govActionProposal.getSlot())
                            .build()).toList());

        }

        // take dRep stake distribution snapshot
        dRepDistService.takeStakeSnapshot(epoch);
    }
}


package com.bloxbean.cardano.yaci.store.governanceaggr.processor;

import com.bloxbean.cardano.yaci.core.model.governance.actions.NoConfidence;
import com.bloxbean.cardano.yaci.core.model.governance.actions.UpdateCommittee;
import com.bloxbean.cardano.yaci.store.client.governance.ProposalStateClient;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionProposal;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionStatus;
import com.bloxbean.cardano.yaci.store.events.internal.PreAdaPotJobProcessingEvent;
import com.bloxbean.cardano.yaci.store.governanceaggr.service.CommitteeStateService;
import com.bloxbean.cardano.yaci.store.governancerules.domain.ConstitutionCommitteeState;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CommitteeStateProcessor {
    private final CommitteeStateService committeeStateService;
    private final ProposalStateClient proposalStateClient;

    @EventListener
    @Transactional
    public void handlePreAdaPotJobProcessingEvent(PreAdaPotJobProcessingEvent event) {
        int epoch = event.getEpoch();

        List<GovActionProposal> ratifiedProposalsInPrevEpoch =
                proposalStateClient.getProposalsByStatusAndEpoch(GovActionStatus.RATIFIED, epoch - 1);

        for (var proposal : ratifiedProposalsInPrevEpoch) {
            if (proposal.getGovAction() instanceof NoConfidence) {
                committeeStateService.saveCommitteeState(ConstitutionCommitteeState.NO_CONFIDENCE, epoch);
            } else if (proposal.getGovAction() instanceof UpdateCommittee) {
                committeeStateService.saveCommitteeState(ConstitutionCommitteeState.NORMAL, epoch);
            }
        }
    }
}

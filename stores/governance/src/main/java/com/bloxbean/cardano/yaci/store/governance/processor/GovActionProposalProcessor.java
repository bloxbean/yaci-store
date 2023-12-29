package com.bloxbean.cardano.yaci.store.governance.processor;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.core.model.governance.ProposalProcedure;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.GovernanceEvent;
import com.bloxbean.cardano.yaci.store.events.domain.TxGovernance;
import com.bloxbean.cardano.yaci.store.governance.domain.GovActionProposal;
import com.bloxbean.cardano.yaci.store.governance.storage.GovActionProposalStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class GovActionProposalProcessor {

    private final GovActionProposalStorage govActionProposalStorage;

    @EventListener
    @Transactional
    public void handleGovernanceAction(GovernanceEvent governanceEvent) {
        EventMetadata eventMetadata = governanceEvent.getMetadata();

        List<GovActionProposal> govActionProposals = new ArrayList<>();
        for (TxGovernance txGovernance : governanceEvent.getTxGovernanceList()) {
            String txHash = txGovernance.getTxHash();

            List<ProposalProcedure> proposalProcedures = txGovernance.getProposalProcedures();

            if (proposalProcedures == null || proposalProcedures.isEmpty()) {
                continue;
            }

            int index = 0;
            for (ProposalProcedure proposalProcedure : proposalProcedures) {
                GovActionProposal govActionProposal = new GovActionProposal();

                govActionProposal.setTxHash(txHash);
                govActionProposal.setIndex(index++);
                govActionProposal.setType(proposalProcedure.getGovAction().getType());
                govActionProposal.setDescription(proposalProcedure.getGovAction().toString());
                govActionProposal.setDeposit(proposalProcedure.getDeposit());

                Address address = new Address(HexUtil.decodeHexString(proposalProcedure.getRewardAccount()));
                govActionProposal.setReturnAddress(address.getAddress());

                if (proposalProcedure.getAnchor() != null) {
                    govActionProposal.setAnchorUrl(proposalProcedure.getAnchor().getAnchor_url());
                    govActionProposal.setAnchorHash(proposalProcedure.getAnchor().getAnchor_data_hash());
                }

                govActionProposal.setBlockNumber(eventMetadata.getBlock());
                govActionProposal.setBlockTime(eventMetadata.getBlockTime());

                govActionProposals.add(govActionProposal);
            }
        }

        if (!govActionProposals.isEmpty()) {
            govActionProposalStorage.saveAll(govActionProposals);
        }
    }
}

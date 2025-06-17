package com.bloxbean.cardano.yaci.store.governance.storage.impl;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.store.governance.domain.GovActionProposal;
import com.bloxbean.cardano.yaci.store.governance.storage.GovActionProposalStorage;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper.GovActionProposalMapper;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.GovActionProposalId;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.repository.GovActionProposalRepository;
import com.bloxbean.cardano.yaci.store.plugin.aspect.Plugin;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class GovActionProposalStorageImpl implements GovActionProposalStorage {

    private static final String PLUGIN_GOV_ACTION_PROPOSAL_SAVE = "governance.gov_action_proposal.save";
    private final GovActionProposalRepository govActionProposalRepository;
    private final GovActionProposalMapper govActionProposalMapper;

    @Override
    @Plugin(key=PLUGIN_GOV_ACTION_PROPOSAL_SAVE)
    public void saveAll(List<GovActionProposal> govActionProposals) {
        govActionProposalRepository.saveAll(govActionProposals.stream()
                .map(govActionProposalMapper::toGovActionProposalEntity)
                .collect(Collectors.toList()));
    }

    @Override
    public int deleteBySlotGreaterThan(long slot) {
        return govActionProposalRepository.deleteBySlotGreaterThan(slot);
    }

    @Override
    public List<GovActionProposal> findByGovActionIds(List<GovActionId> govActionIds) {
        return govActionProposalRepository.findAllById(govActionIds.stream().map(govActionId -> {
            GovActionProposalId govActionProposalId = new GovActionProposalId();
            govActionProposalId.setTxHash(govActionId.getTransactionId());
            govActionProposalId.setIndex(govActionId.getGov_action_index());

            return govActionProposalId;
        }).toList()).stream().map(govActionProposalMapper::toGovActionProposal).toList();
    }

    @Override
    public List<GovActionProposal> findByEpoch(int epoch) {
        return govActionProposalRepository.findByEpoch(epoch).stream()
                .map(govActionProposalMapper::toGovActionProposal)
                .collect(Collectors.toList());
    }

}

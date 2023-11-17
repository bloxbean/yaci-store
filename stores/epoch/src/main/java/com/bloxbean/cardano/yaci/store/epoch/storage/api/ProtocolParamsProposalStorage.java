package com.bloxbean.cardano.yaci.store.epoch.storage.api;

import com.bloxbean.cardano.yaci.store.epoch.domain.ProtocolParamsProposal;

import java.util.List;

public interface ProtocolParamsProposalStorage {
    void saveAll(List<ProtocolParamsProposal> protocolParamsProposals);
    List<ProtocolParamsProposal> getProtocolParamsProposalsByTargetEpoch(int epoch);

    int deleteBySlotGreaterThan(long slot);
}

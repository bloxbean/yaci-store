package com.bloxbean.cardano.yaci.store.protocolparams.storage.api;

import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.protocolparams.domain.EpochParam;
import com.bloxbean.cardano.yaci.store.protocolparams.domain.ProtocolParamsProposal;

import java.util.List;

public interface ProtocolParamsProposalStorage {
    void saveAll(List<ProtocolParamsProposal> protocolParamsProposals);
    List<ProtocolParamsProposal> getProtocolParamsProposals(int page, int count, Order order);

    List<ProtocolParamsProposal> getProtocolParamsProposalsByTargetEpoch(int epoch);
    List<ProtocolParamsProposal> getProtocolParamsProposalsByCreateEpoch(int epoch);

    int deleteBySlotGreaterThan(long slot);
}

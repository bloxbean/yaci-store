package com.bloxbean.cardano.yaci.store.epoch.storage;

import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.epoch.domain.ProtocolParamsProposal;

import java.util.List;

public interface ProtocolParamsProposalStorageReader {
    List<ProtocolParamsProposal> getProtocolParamsProposals(int page, int count, Order order);
    List<ProtocolParamsProposal> getProtocolParamsProposalsByCreateEpoch(int epoch);
}

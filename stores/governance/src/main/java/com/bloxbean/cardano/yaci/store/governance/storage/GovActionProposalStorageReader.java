package com.bloxbean.cardano.yaci.store.governance.storage;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.governance.domain.GovActionProposal;

import java.util.List;
import java.util.Optional;

public interface GovActionProposalStorageReader {

    List<GovActionProposal> findAll(int page, int count, Order order);

    List<GovActionProposal> findByTxHash(String txHash);

    List<GovActionProposal> findByGovActionType(GovActionType govActionType, int page, int count, Order order);

    List<GovActionProposal> findByReturnAddress(String address, int page, int count, Order order);

    Optional<GovActionProposal> findMostRecentGovActionByType(GovActionType govActionType);
}

package com.bloxbean.cardano.yaci.store.governanceaggr.storage;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionStatus;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.GovActionProposalStatus;

import java.util.List;
import java.util.Optional;

public interface GovActionProposalStatusStorage {
    void saveAll(List<GovActionProposalStatus> govActionProposalStatusList);
    void deleteByEpoch(Integer epoch);
    List<GovActionProposalStatus> findByStatusAndEpoch(GovActionStatus status, int epoch);
    List<GovActionProposalStatus> findByStatusListAndEpoch(List<GovActionStatus> statusList, int epoch);
    List<GovActionProposalStatus> findByTypeAndStatusAndEpochLessThan(GovActionType govActionType, GovActionStatus status, int epoch);
    Optional<GovActionProposalStatus> findLastEnactedProposal(GovActionType govActionType, int epoch);
    Optional<Integer> findLatestEpochWithStatusBefore(List<GovActionStatus> statusList, int epoch);
}

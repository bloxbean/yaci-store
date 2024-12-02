package com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl;

import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.GovActionStatus;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.GovActionProposalStatus;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.GovActionProposalStatusStorage;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.mapper.GovActionProposalStatusMapper;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.repository.GovActionProposalStatusRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class GovActionProposalStatusStorageImpl implements GovActionProposalStatusStorage {
    private final GovActionProposalStatusRepository govActionProposalStatusRepository;
    private final GovActionProposalStatusMapper mapper;

    @Override
    public void saveAll(List<GovActionProposalStatus> govActionProposalStatusList) {
        govActionProposalStatusRepository.saveAll(govActionProposalStatusList.stream()
                .map(mapper::toGovActionProposalStatusEntity).toList());
    }

    @Override
    public List<GovActionProposalStatus> findByStatusAndEpochLessThanEqual(GovActionStatus status, int epoch) {
        return govActionProposalStatusRepository.findByStatusAndEpochLessThan(status, epoch).stream()
                .map(mapper::toGovActionProposalStatus).toList();
    }

    @Override
    public int deleteBySlotGreaterThan(long slot) {
        return govActionProposalStatusRepository.deleteBySlotGreaterThan(slot);
    }
}

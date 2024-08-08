package com.bloxbean.cardano.yaci.store.governance.storage.impl;

import com.bloxbean.cardano.yaci.store.governance.domain.LocalGovActionProposalStatus;
import com.bloxbean.cardano.yaci.store.governance.storage.LocalGovActionProposalStatusStorage;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper.LocalGovActionProposalStatusMapper;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.repository.LocalGovActionProposalStatusRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class LocalGovActionProposalStatusStorageImpl implements LocalGovActionProposalStatusStorage {
    private final LocalGovActionProposalStatusRepository localGovActionProposalStatusRepository;
    private final LocalGovActionProposalStatusMapper localGovActionProposalStatusMapper;

    @Override
    public void saveAll(List<LocalGovActionProposalStatus> localGovActionProposalStatusList) {
        localGovActionProposalStatusRepository.saveAll(localGovActionProposalStatusList.stream()
                .map(localGovActionProposalStatusMapper::toLocalGovActionProposalStatusEntity).toList());
    }

    @Override
    public int deleteBySlotGreaterThan(long slot) {
        return localGovActionProposalStatusRepository.deleteBySlotGreaterThan(slot);
    }
}

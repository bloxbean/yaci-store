package com.bloxbean.cardano.yaci.store.governance.storage.impl;

import com.bloxbean.cardano.yaci.store.governance.domain.Committee;
import com.bloxbean.cardano.yaci.store.governance.storage.CommitteeStorage;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper.CommitteeMapper;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.repository.CommitteeRepository;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class CommitteeStorageImpl implements CommitteeStorage {
    private final CommitteeRepository committeeRepository;
    private final CommitteeMapper committeeMapper;

    @Override
    public void save(Committee committee) {
        committeeRepository.save(committeeMapper.toCommitteeEntity(committee));
    }

    @Override
    public Optional<Committee> getCommitteeByEpoch(int epoch) {
        return committeeRepository.findCommitteeByEpoch(epoch)
                .map(committeeEntity -> committeeMapper.toCommittee(committeeEntity));
    }

    @Override
    public int deleteBySlotGreaterThan(long slot) {
        return committeeRepository.deleteBySlotGreaterThan(slot);
    }
}

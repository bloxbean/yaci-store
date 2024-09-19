package com.bloxbean.cardano.yaci.store.governance.storage.local.impl;

import com.bloxbean.cardano.yaci.store.governance.domain.local.LocalCommittee;
import com.bloxbean.cardano.yaci.store.governance.storage.local.LocalCommitteeStorage;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper.LocalCommitteeMapper;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.repository.LocalCommitteeRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LocalCommitteeStorageImpl implements LocalCommitteeStorage {
    private final LocalCommitteeRepository localCommitteeRepository;
    private final LocalCommitteeMapper localCommitteeMapper;

    @Override
    public void save(LocalCommittee localCommittee) {
        localCommitteeRepository.save(localCommitteeMapper.toLocalCommitteeEntity(localCommittee));
    }

    @Override
    public int deleteBySlotGreaterThan(long slot) {
        return localCommitteeRepository.deleteBySlotGreaterThan(slot);
    }
}

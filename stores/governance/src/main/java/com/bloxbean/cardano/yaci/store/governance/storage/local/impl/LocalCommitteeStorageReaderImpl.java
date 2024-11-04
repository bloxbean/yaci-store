package com.bloxbean.cardano.yaci.store.governance.storage.local.impl;

import com.bloxbean.cardano.yaci.store.governance.domain.local.LocalCommittee;
import com.bloxbean.cardano.yaci.store.governance.storage.local.LocalCommitteeStorageReader;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper.LocalCommitteeMapper;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.repository.LocalCommitteeRepository;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class LocalCommitteeStorageReaderImpl implements LocalCommitteeStorageReader {
    private final LocalCommitteeRepository localCommitteeRepository;
    private final LocalCommitteeMapper localCommitteeMapper;

    @Override
    public Optional<LocalCommittee> findByMaxSlot() {
        return localCommitteeRepository.findFirstByOrderBySlotDesc()
                .map(localCommitteeMapper::toLocalCommittee);
    }
}

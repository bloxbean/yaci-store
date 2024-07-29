package com.bloxbean.cardano.yaci.store.governance.storage.impl;

import com.bloxbean.cardano.yaci.store.governance.domain.Committee;
import com.bloxbean.cardano.yaci.store.governance.storage.CommitteeStorageReader;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper.CommitteeMapper;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.repository.CommitteeRepository;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class CommitteeStorageReaderImpl implements CommitteeStorageReader {
    private final CommitteeRepository committeeRepository;
    private final CommitteeMapper committeeMapper;

    @Override
    public Optional<Committee> findByMaxEpoch() {
        return committeeRepository.findFirstByOrderByEpochDesc()
                .map(committeeMapper::toCommittee);
    }
}

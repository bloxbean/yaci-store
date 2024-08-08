package com.bloxbean.cardano.yaci.store.governance.storage.impl;

import com.bloxbean.cardano.yaci.store.governance.domain.LocalGovActionProposalStatus;
import com.bloxbean.cardano.yaci.store.governance.storage.LocalGovActionProposalStatusStorageReader;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper.LocalGovActionProposalStatusMapper;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.GovActionStatus;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.repository.LocalGovActionProposalStatusRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class LocalGovActionProposalStatusStorageReaderImpl implements LocalGovActionProposalStatusStorageReader {
    private final LocalGovActionProposalStatusRepository localGovActionProposalStatusRepository;
    private final LocalGovActionProposalStatusMapper localGovActionProposalStatusMapper;

    @Override
    public List<LocalGovActionProposalStatus> findByEpochAndStatusIn(Integer epochNo, List<GovActionStatus> expired) {
        return localGovActionProposalStatusRepository.findByEpochAndStatusIn(epochNo, expired)
                .stream()
                .map(localGovActionProposalStatusMapper::toLocalGovActionProposalStatus)
                .toList();
    }
}

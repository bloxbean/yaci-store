package com.bloxbean.cardano.yaci.store.api.governance.service;

import com.bloxbean.cardano.yaci.store.api.governance.dto.LocalCommitteeDto;
import com.bloxbean.cardano.yaci.store.api.governance.dto.LocalCommitteeMemberDto;
import com.bloxbean.cardano.yaci.store.governance.domain.LocalCommittee;
import com.bloxbean.cardano.yaci.store.governance.domain.LocalCommitteeMember;
import com.bloxbean.cardano.yaci.store.governance.storage.LocalCommitteeMemberStorageReader;
import com.bloxbean.cardano.yaci.store.governance.storage.LocalCommitteeStorageReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LocalCommitteeService {
    private final LocalCommitteeStorageReader localCommitteeStorageReader;
    private final LocalCommitteeMemberStorageReader localCommitteeMemberStorageReader;

    public Optional<LocalCommitteeDto> getCurrentCommittee() {
        Optional<LocalCommittee> committee = localCommitteeStorageReader.findByMaxSlot();
        if (committee.isEmpty()) {
            return Optional.empty();
        }

        List<LocalCommitteeMember> committeeMembers = localCommitteeMemberStorageReader.findCommitteeMembersWithMaxSlot();

        return Optional.of(LocalCommitteeDto.builder()
                .threshold(committee.get().getThreshold())
                .members(committeeMembers.stream().map(committeeMember -> LocalCommitteeMemberDto.builder()
                        .hash(committeeMember.getHash())
                        .credType(committeeMember.getCredType())
                        .expiredEpoch(committeeMember.getExpiredEpoch())
                        .build()).toList())
                .build());
    }
}

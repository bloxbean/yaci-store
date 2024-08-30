package com.bloxbean.cardano.yaci.store.api.governance.service;

import com.bloxbean.cardano.yaci.store.api.governance.dto.CommitteeDto;
import com.bloxbean.cardano.yaci.store.api.governance.dto.CommitteeMemberDto;
import com.bloxbean.cardano.yaci.store.governance.domain.Committee;
import com.bloxbean.cardano.yaci.store.governance.domain.CommitteeMember;
import com.bloxbean.cardano.yaci.store.governance.storage.CommitteeMemberStorageReader;
import com.bloxbean.cardano.yaci.store.governance.storage.CommitteeStorageReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CommitteeService {
    private final CommitteeStorageReader committeeStorageReader;
    private final CommitteeMemberStorageReader committeeMemberStorageReader;

    public Optional<CommitteeDto> getCurrentCommittee() {
        Optional<Committee> committee = committeeStorageReader.findByMaxEpoch();
        if (committee.isEmpty()) {
            return Optional.empty();
        }

        List<CommitteeMember> committeeMembers = committeeMemberStorageReader.findCommitteeMembersWithMaxSlot();

        return Optional.of(CommitteeDto.builder()
                .thresholdNumerator(committee.get().getThresholdNumerator())
                .thresholdDenominator(committee.get().getThresholdDenominator())
                .members(committeeMembers.stream().map(committeeMember -> CommitteeMemberDto.builder()
                        .hash(committeeMember.getHash())
                        .credType(committeeMember.getCredType())
                        .startEpoch(committeeMember.getStartEpoch())
                        .expiredEpoch(committeeMember.getExpiredEpoch())
                        .build()).toList())
                .build());
    }
}

package com.bloxbean.cardano.yaci.store.api.governance.service.local;

import com.bloxbean.cardano.yaci.store.api.governance.dto.local.LocalCommitteeDto;
import com.bloxbean.cardano.yaci.store.api.governance.dto.local.LocalCommitteeMemberDto;
import com.bloxbean.cardano.yaci.store.governance.domain.local.LocalCommittee;
import com.bloxbean.cardano.yaci.store.governance.domain.local.LocalCommitteeMember;
import com.bloxbean.cardano.yaci.store.governance.service.LocalGovStateService;
import com.bloxbean.cardano.yaci.store.governance.storage.local.LocalCommitteeMemberStorageReader;
import com.bloxbean.cardano.yaci.store.governance.storage.local.LocalCommitteeStorageReader;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@ConditionalOnBean(LocalGovStateService.class)
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

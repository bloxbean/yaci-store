package com.bloxbean.cardano.yaci.store.governance.storage.local;

import com.bloxbean.cardano.yaci.store.governance.domain.local.LocalCommitteeMember;

import java.util.List;

public interface LocalCommitteeMemberStorage {
    void saveAll(List<LocalCommitteeMember> localCommitteeMembers);

    int deleteBySlotGreaterThan(long slot);
}

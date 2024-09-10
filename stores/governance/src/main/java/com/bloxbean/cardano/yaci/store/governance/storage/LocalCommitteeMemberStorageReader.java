package com.bloxbean.cardano.yaci.store.governance.storage;

import com.bloxbean.cardano.yaci.store.governance.domain.LocalCommitteeMember;

import java.util.List;

public interface LocalCommitteeMemberStorageReader {
    List<LocalCommitteeMember> findCommitteeMembersWithMaxSlot();
}

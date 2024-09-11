package com.bloxbean.cardano.yaci.store.governance.storage.local;

import com.bloxbean.cardano.yaci.store.governance.domain.local.LocalCommitteeMember;

import java.util.List;

public interface LocalCommitteeMemberStorageReader {
    List<LocalCommitteeMember> findCommitteeMembersWithMaxSlot();
}

package com.bloxbean.cardano.yaci.store.governance.storage;

import com.bloxbean.cardano.yaci.store.governance.domain.CommitteeMember;

import java.util.List;

public interface CommitteeMemberStorageReader {
    List<CommitteeMember> findCurrentMembers();
}

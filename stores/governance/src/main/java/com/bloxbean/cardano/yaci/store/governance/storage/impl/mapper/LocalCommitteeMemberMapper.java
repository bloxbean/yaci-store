package com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.governance.domain.local.LocalCommitteeMember;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.LocalCommitteeMemberEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class LocalCommitteeMemberMapper {
    public abstract LocalCommitteeMemberEntity toLocalCommitteeMemberEntity(LocalCommitteeMember localCommitteeMember);

    public abstract LocalCommitteeMember toLocalCommitteeMember(LocalCommitteeMemberEntity localCommitteeMemberEntity);
}

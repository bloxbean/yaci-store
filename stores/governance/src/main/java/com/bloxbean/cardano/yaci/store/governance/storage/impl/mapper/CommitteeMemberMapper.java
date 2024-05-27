package com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.governance.domain.CommitteeMember;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.CommitteeMemberEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class CommitteeMemberMapper {
    public abstract CommitteeMemberEntity toCommitteeMemberEntity(CommitteeMember committeeMember);
    public abstract CommitteeMember toCommitteeMember(CommitteeMemberEntity committeeMemberEntity);
}

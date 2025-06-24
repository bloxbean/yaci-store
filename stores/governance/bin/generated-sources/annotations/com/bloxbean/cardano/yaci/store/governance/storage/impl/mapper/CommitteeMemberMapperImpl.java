package com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.governance.domain.CommitteeMember;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.CommitteeMemberEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-20T15:09:18+0100",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.v20250514-1000, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class CommitteeMemberMapperImpl extends CommitteeMemberMapper {

    @Override
    public CommitteeMemberEntity toCommitteeMemberEntity(CommitteeMember committeeMember) {
        if ( committeeMember == null ) {
            return null;
        }

        CommitteeMemberEntity.CommitteeMemberEntityBuilder<?, ?> committeeMemberEntity = CommitteeMemberEntity.builder();

        committeeMemberEntity.credType( committeeMember.getCredType() );
        committeeMemberEntity.epoch( committeeMember.getEpoch() );
        committeeMemberEntity.expiredEpoch( committeeMember.getExpiredEpoch() );
        committeeMemberEntity.hash( committeeMember.getHash() );
        committeeMemberEntity.slot( committeeMember.getSlot() );
        committeeMemberEntity.startEpoch( committeeMember.getStartEpoch() );

        return committeeMemberEntity.build();
    }

    @Override
    public CommitteeMember toCommitteeMember(CommitteeMemberEntity committeeMemberEntity) {
        if ( committeeMemberEntity == null ) {
            return null;
        }

        CommitteeMember.CommitteeMemberBuilder<?, ?> committeeMember = CommitteeMember.builder();

        committeeMember.credType( committeeMemberEntity.getCredType() );
        committeeMember.epoch( committeeMemberEntity.getEpoch() );
        committeeMember.expiredEpoch( committeeMemberEntity.getExpiredEpoch() );
        committeeMember.hash( committeeMemberEntity.getHash() );
        committeeMember.slot( committeeMemberEntity.getSlot() );
        committeeMember.startEpoch( committeeMemberEntity.getStartEpoch() );

        return committeeMember.build();
    }
}

package com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.governance.domain.local.LocalCommitteeMember;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.LocalCommitteeMemberEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-20T15:09:18+0100",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.v20250514-1000, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class LocalCommitteeMemberMapperImpl extends LocalCommitteeMemberMapper {

    @Override
    public LocalCommitteeMemberEntity toLocalCommitteeMemberEntity(LocalCommitteeMember localCommitteeMember) {
        if ( localCommitteeMember == null ) {
            return null;
        }

        LocalCommitteeMemberEntity.LocalCommitteeMemberEntityBuilder<?, ?> localCommitteeMemberEntity = LocalCommitteeMemberEntity.builder();

        localCommitteeMemberEntity.credType( localCommitteeMember.getCredType() );
        localCommitteeMemberEntity.epoch( localCommitteeMember.getEpoch() );
        localCommitteeMemberEntity.expiredEpoch( localCommitteeMember.getExpiredEpoch() );
        localCommitteeMemberEntity.hash( localCommitteeMember.getHash() );
        localCommitteeMemberEntity.slot( localCommitteeMember.getSlot() );

        return localCommitteeMemberEntity.build();
    }

    @Override
    public LocalCommitteeMember toLocalCommitteeMember(LocalCommitteeMemberEntity localCommitteeMemberEntity) {
        if ( localCommitteeMemberEntity == null ) {
            return null;
        }

        LocalCommitteeMember.LocalCommitteeMemberBuilder localCommitteeMember = LocalCommitteeMember.builder();

        localCommitteeMember.credType( localCommitteeMemberEntity.getCredType() );
        localCommitteeMember.epoch( localCommitteeMemberEntity.getEpoch() );
        localCommitteeMember.expiredEpoch( localCommitteeMemberEntity.getExpiredEpoch() );
        localCommitteeMember.hash( localCommitteeMemberEntity.getHash() );
        localCommitteeMember.slot( localCommitteeMemberEntity.getSlot() );

        return localCommitteeMember.build();
    }
}

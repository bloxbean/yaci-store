package com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.governance.domain.DelegationVote;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.DelegationVoteEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-20T15:09:19+0100",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.v20250514-1000, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class DelegationVoteMapperImpl extends DelegationVoteMapper {

    @Override
    public DelegationVoteEntity toDelegationVoteEntity(DelegationVote delegationVote) {
        if ( delegationVote == null ) {
            return null;
        }

        DelegationVoteEntity.DelegationVoteEntityBuilder<?, ?> delegationVoteEntity = DelegationVoteEntity.builder();

        delegationVoteEntity.blockNumber( delegationVote.getBlockNumber() );
        delegationVoteEntity.blockTime( delegationVote.getBlockTime() );
        delegationVoteEntity.address( delegationVote.getAddress() );
        delegationVoteEntity.certIndex( delegationVote.getCertIndex() );
        delegationVoteEntity.credType( delegationVote.getCredType() );
        delegationVoteEntity.credential( delegationVote.getCredential() );
        delegationVoteEntity.drepHash( delegationVote.getDrepHash() );
        delegationVoteEntity.drepId( delegationVote.getDrepId() );
        delegationVoteEntity.drepType( delegationVote.getDrepType() );
        delegationVoteEntity.epoch( delegationVote.getEpoch() );
        delegationVoteEntity.slot( delegationVote.getSlot() );
        delegationVoteEntity.txHash( delegationVote.getTxHash() );
        delegationVoteEntity.txIndex( delegationVote.getTxIndex() );

        return delegationVoteEntity.build();
    }

    @Override
    public DelegationVote toDelegationVote(DelegationVoteEntity delegationVoteEntity) {
        if ( delegationVoteEntity == null ) {
            return null;
        }

        DelegationVote.DelegationVoteBuilder<?, ?> delegationVote = DelegationVote.builder();

        delegationVote.blockNumber( delegationVoteEntity.getBlockNumber() );
        delegationVote.blockTime( delegationVoteEntity.getBlockTime() );
        delegationVote.address( delegationVoteEntity.getAddress() );
        delegationVote.certIndex( delegationVoteEntity.getCertIndex() );
        delegationVote.credType( delegationVoteEntity.getCredType() );
        delegationVote.credential( delegationVoteEntity.getCredential() );
        delegationVote.drepHash( delegationVoteEntity.getDrepHash() );
        delegationVote.drepId( delegationVoteEntity.getDrepId() );
        delegationVote.drepType( delegationVoteEntity.getDrepType() );
        delegationVote.epoch( delegationVoteEntity.getEpoch() );
        delegationVote.slot( delegationVoteEntity.getSlot() );
        delegationVote.txHash( delegationVoteEntity.getTxHash() );
        delegationVote.txIndex( delegationVoteEntity.getTxIndex() );

        return delegationVote.build();
    }
}

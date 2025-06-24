package com.bloxbean.cardano.yaci.store.staking.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.staking.domain.Delegation;
import com.bloxbean.cardano.yaci.store.staking.domain.StakeRegistrationDetail;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.model.DelegationEntity;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.model.StakeRegistrationEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-20T15:09:23+0100",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.v20250514-1000, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class StakingMapperImpl extends StakingMapper {

    @Override
    public StakeRegistrationEntity toStakeResistrationEntity(StakeRegistrationDetail stakeRegistrationDetail) {
        if ( stakeRegistrationDetail == null ) {
            return null;
        }

        StakeRegistrationEntity.StakeRegistrationEntityBuilder<?, ?> stakeRegistrationEntity = StakeRegistrationEntity.builder();

        stakeRegistrationEntity.blockNumber( stakeRegistrationDetail.getBlockNumber() );
        stakeRegistrationEntity.blockTime( stakeRegistrationDetail.getBlockTime() );
        stakeRegistrationEntity.address( stakeRegistrationDetail.getAddress() );
        stakeRegistrationEntity.blockHash( stakeRegistrationDetail.getBlockHash() );
        stakeRegistrationEntity.certIndex( stakeRegistrationDetail.getCertIndex() );
        stakeRegistrationEntity.credential( stakeRegistrationDetail.getCredential() );
        stakeRegistrationEntity.credentialType( stakeRegistrationDetail.getCredentialType() );
        stakeRegistrationEntity.epoch( stakeRegistrationDetail.getEpoch() );
        stakeRegistrationEntity.slot( stakeRegistrationDetail.getSlot() );
        stakeRegistrationEntity.txHash( stakeRegistrationDetail.getTxHash() );
        stakeRegistrationEntity.txIndex( stakeRegistrationDetail.getTxIndex() );
        stakeRegistrationEntity.type( stakeRegistrationDetail.getType() );

        return stakeRegistrationEntity.build();
    }

    @Override
    public StakeRegistrationDetail toStakeRegistrationDetail(StakeRegistrationEntity stakeRegistrationEntity) {
        if ( stakeRegistrationEntity == null ) {
            return null;
        }

        StakeRegistrationDetail.StakeRegistrationDetailBuilder<?, ?> stakeRegistrationDetail = StakeRegistrationDetail.builder();

        stakeRegistrationDetail.blockNumber( stakeRegistrationEntity.getBlockNumber() );
        stakeRegistrationDetail.blockTime( stakeRegistrationEntity.getBlockTime() );
        stakeRegistrationDetail.address( stakeRegistrationEntity.getAddress() );
        stakeRegistrationDetail.blockHash( stakeRegistrationEntity.getBlockHash() );
        stakeRegistrationDetail.certIndex( (int) stakeRegistrationEntity.getCertIndex() );
        stakeRegistrationDetail.credential( stakeRegistrationEntity.getCredential() );
        stakeRegistrationDetail.credentialType( stakeRegistrationEntity.getCredentialType() );
        if ( stakeRegistrationEntity.getEpoch() != null ) {
            stakeRegistrationDetail.epoch( stakeRegistrationEntity.getEpoch() );
        }
        if ( stakeRegistrationEntity.getSlot() != null ) {
            stakeRegistrationDetail.slot( stakeRegistrationEntity.getSlot() );
        }
        stakeRegistrationDetail.txHash( stakeRegistrationEntity.getTxHash() );
        stakeRegistrationDetail.txIndex( stakeRegistrationEntity.getTxIndex() );
        stakeRegistrationDetail.type( stakeRegistrationEntity.getType() );

        return stakeRegistrationDetail.build();
    }

    @Override
    public DelegationEntity toDelegationEntity(Delegation delegation) {
        if ( delegation == null ) {
            return null;
        }

        DelegationEntity.DelegationEntityBuilder<?, ?> delegationEntity = DelegationEntity.builder();

        delegationEntity.blockNumber( delegation.getBlockNumber() );
        delegationEntity.blockTime( delegation.getBlockTime() );
        delegationEntity.address( delegation.getAddress() );
        delegationEntity.blockHash( delegation.getBlockHash() );
        delegationEntity.certIndex( delegation.getCertIndex() );
        delegationEntity.credential( delegation.getCredential() );
        delegationEntity.credentialType( delegation.getCredentialType() );
        delegationEntity.epoch( delegation.getEpoch() );
        delegationEntity.poolId( delegation.getPoolId() );
        delegationEntity.slot( delegation.getSlot() );
        delegationEntity.txHash( delegation.getTxHash() );
        delegationEntity.txIndex( delegation.getTxIndex() );

        return delegationEntity.build();
    }

    @Override
    public Delegation toDelegation(DelegationEntity delegationEntity) {
        if ( delegationEntity == null ) {
            return null;
        }

        Delegation.DelegationBuilder<?, ?> delegation = Delegation.builder();

        delegation.blockNumber( delegationEntity.getBlockNumber() );
        delegation.blockTime( delegationEntity.getBlockTime() );
        delegation.address( delegationEntity.getAddress() );
        delegation.blockHash( delegationEntity.getBlockHash() );
        delegation.certIndex( (int) delegationEntity.getCertIndex() );
        delegation.credential( delegationEntity.getCredential() );
        delegation.credentialType( delegationEntity.getCredentialType() );
        if ( delegationEntity.getEpoch() != null ) {
            delegation.epoch( delegationEntity.getEpoch() );
        }
        delegation.poolId( delegationEntity.getPoolId() );
        if ( delegationEntity.getSlot() != null ) {
            delegation.slot( delegationEntity.getSlot() );
        }
        delegation.txHash( delegationEntity.getTxHash() );
        delegation.txIndex( delegationEntity.getTxIndex() );

        return delegation.build();
    }
}

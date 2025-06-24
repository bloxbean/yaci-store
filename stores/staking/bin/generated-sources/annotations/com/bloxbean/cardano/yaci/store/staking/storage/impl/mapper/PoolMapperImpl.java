package com.bloxbean.cardano.yaci.store.staking.storage.impl.mapper;

import com.bloxbean.cardano.yaci.core.model.Relay;
import com.bloxbean.cardano.yaci.store.staking.domain.Pool;
import com.bloxbean.cardano.yaci.store.staking.domain.PoolRegistration;
import com.bloxbean.cardano.yaci.store.staking.domain.PoolRetirement;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.model.PoolEntity;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.model.PoolRegistrationEnity;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.model.PoolRetirementEntity;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-20T15:09:23+0100",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.v20250514-1000, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class PoolMapperImpl extends PoolMapper {

    @Override
    public PoolRegistrationEnity toPoolRegistrationEntity(PoolRegistration poolRegistrationDetail) {
        if ( poolRegistrationDetail == null ) {
            return null;
        }

        PoolRegistrationEnity.PoolRegistrationEnityBuilder<?, ?> poolRegistrationEnity = PoolRegistrationEnity.builder();

        poolRegistrationEnity.blockNumber( poolRegistrationDetail.getBlockNumber() );
        poolRegistrationEnity.blockTime( poolRegistrationDetail.getBlockTime() );
        poolRegistrationEnity.blockHash( poolRegistrationDetail.getBlockHash() );
        poolRegistrationEnity.certIndex( poolRegistrationDetail.getCertIndex() );
        poolRegistrationEnity.cost( poolRegistrationDetail.getCost() );
        poolRegistrationEnity.epoch( poolRegistrationDetail.getEpoch() );
        poolRegistrationEnity.margin( poolRegistrationDetail.getMargin() );
        poolRegistrationEnity.marginDenominator( poolRegistrationDetail.getMarginDenominator() );
        poolRegistrationEnity.marginNumerator( poolRegistrationDetail.getMarginNumerator() );
        poolRegistrationEnity.metadataHash( poolRegistrationDetail.getMetadataHash() );
        poolRegistrationEnity.metadataUrl( poolRegistrationDetail.getMetadataUrl() );
        poolRegistrationEnity.pledge( poolRegistrationDetail.getPledge() );
        poolRegistrationEnity.poolId( poolRegistrationDetail.getPoolId() );
        Set<String> set = poolRegistrationDetail.getPoolOwners();
        if ( set != null ) {
            poolRegistrationEnity.poolOwners( new LinkedHashSet<String>( set ) );
        }
        List<Relay> list = poolRegistrationDetail.getRelays();
        if ( list != null ) {
            poolRegistrationEnity.relays( new ArrayList<Relay>( list ) );
        }
        poolRegistrationEnity.rewardAccount( poolRegistrationDetail.getRewardAccount() );
        poolRegistrationEnity.slot( poolRegistrationDetail.getSlot() );
        poolRegistrationEnity.txHash( poolRegistrationDetail.getTxHash() );
        poolRegistrationEnity.txIndex( poolRegistrationDetail.getTxIndex() );
        poolRegistrationEnity.vrfKeyHash( poolRegistrationDetail.getVrfKeyHash() );

        return poolRegistrationEnity.build();
    }

    @Override
    public PoolRegistration toPoolRegistration(PoolRegistrationEnity poolRegistrationEnity) {
        if ( poolRegistrationEnity == null ) {
            return null;
        }

        PoolRegistration.PoolRegistrationBuilder<?, ?> poolRegistration = PoolRegistration.builder();

        poolRegistration.blockNumber( poolRegistrationEnity.getBlockNumber() );
        poolRegistration.blockTime( poolRegistrationEnity.getBlockTime() );
        poolRegistration.blockHash( poolRegistrationEnity.getBlockHash() );
        poolRegistration.certIndex( poolRegistrationEnity.getCertIndex() );
        poolRegistration.cost( poolRegistrationEnity.getCost() );
        if ( poolRegistrationEnity.getEpoch() != null ) {
            poolRegistration.epoch( poolRegistrationEnity.getEpoch() );
        }
        if ( poolRegistrationEnity.getMargin() != null ) {
            poolRegistration.margin( poolRegistrationEnity.getMargin() );
        }
        poolRegistration.marginDenominator( poolRegistrationEnity.getMarginDenominator() );
        poolRegistration.marginNumerator( poolRegistrationEnity.getMarginNumerator() );
        poolRegistration.metadataHash( poolRegistrationEnity.getMetadataHash() );
        poolRegistration.metadataUrl( poolRegistrationEnity.getMetadataUrl() );
        poolRegistration.pledge( poolRegistrationEnity.getPledge() );
        poolRegistration.poolId( poolRegistrationEnity.getPoolId() );
        Set<String> set = poolRegistrationEnity.getPoolOwners();
        if ( set != null ) {
            poolRegistration.poolOwners( new LinkedHashSet<String>( set ) );
        }
        List<Relay> list = poolRegistrationEnity.getRelays();
        if ( list != null ) {
            poolRegistration.relays( new ArrayList<Relay>( list ) );
        }
        poolRegistration.rewardAccount( poolRegistrationEnity.getRewardAccount() );
        if ( poolRegistrationEnity.getSlot() != null ) {
            poolRegistration.slot( poolRegistrationEnity.getSlot() );
        }
        poolRegistration.txHash( poolRegistrationEnity.getTxHash() );
        poolRegistration.txIndex( poolRegistrationEnity.getTxIndex() );
        poolRegistration.vrfKeyHash( poolRegistrationEnity.getVrfKeyHash() );

        return poolRegistration.build();
    }

    @Override
    public PoolRetirementEntity toPoolRetirementEntity(PoolRetirement poolRetirement) {
        if ( poolRetirement == null ) {
            return null;
        }

        PoolRetirementEntity.PoolRetirementEntityBuilder<?, ?> poolRetirementEntity = PoolRetirementEntity.builder();

        poolRetirementEntity.blockNumber( poolRetirement.getBlockNumber() );
        poolRetirementEntity.blockTime( poolRetirement.getBlockTime() );
        poolRetirementEntity.blockHash( poolRetirement.getBlockHash() );
        poolRetirementEntity.certIndex( poolRetirement.getCertIndex() );
        poolRetirementEntity.epoch( poolRetirement.getEpoch() );
        poolRetirementEntity.poolId( poolRetirement.getPoolId() );
        poolRetirementEntity.retirementEpoch( poolRetirement.getRetirementEpoch() );
        poolRetirementEntity.slot( poolRetirement.getSlot() );
        poolRetirementEntity.txHash( poolRetirement.getTxHash() );
        poolRetirementEntity.txIndex( poolRetirement.getTxIndex() );

        return poolRetirementEntity.build();
    }

    @Override
    public PoolRetirement toPoolRetirement(PoolRetirementEntity poolRetirementEntity) {
        if ( poolRetirementEntity == null ) {
            return null;
        }

        PoolRetirement.PoolRetirementBuilder<?, ?> poolRetirement = PoolRetirement.builder();

        poolRetirement.blockNumber( poolRetirementEntity.getBlockNumber() );
        poolRetirement.blockTime( poolRetirementEntity.getBlockTime() );
        poolRetirement.blockHash( poolRetirementEntity.getBlockHash() );
        poolRetirement.certIndex( poolRetirementEntity.getCertIndex() );
        if ( poolRetirementEntity.getEpoch() != null ) {
            poolRetirement.epoch( poolRetirementEntity.getEpoch() );
        }
        poolRetirement.poolId( poolRetirementEntity.getPoolId() );
        poolRetirement.retirementEpoch( poolRetirementEntity.getRetirementEpoch() );
        if ( poolRetirementEntity.getSlot() != null ) {
            poolRetirement.slot( poolRetirementEntity.getSlot() );
        }
        poolRetirement.txHash( poolRetirementEntity.getTxHash() );
        poolRetirement.txIndex( poolRetirementEntity.getTxIndex() );

        return poolRetirement.build();
    }

    @Override
    public Pool toDeposit(PoolEntity depositEntity) {
        if ( depositEntity == null ) {
            return null;
        }

        Pool.PoolBuilder<?, ?> pool = Pool.builder();

        pool.blockNumber( depositEntity.getBlockNumber() );
        pool.blockTime( depositEntity.getBlockTime() );
        pool.activeEpoch( depositEntity.getActiveEpoch() );
        pool.amount( depositEntity.getAmount() );
        pool.blockHash( depositEntity.getBlockHash() );
        pool.certIndex( depositEntity.getCertIndex() );
        pool.epoch( depositEntity.getEpoch() );
        pool.poolId( depositEntity.getPoolId() );
        pool.registrationSlot( depositEntity.getRegistrationSlot() );
        pool.retireEpoch( depositEntity.getRetireEpoch() );
        pool.slot( depositEntity.getSlot() );
        pool.status( depositEntity.getStatus() );
        pool.txHash( depositEntity.getTxHash() );
        pool.txIndex( depositEntity.getTxIndex() );

        return pool.build();
    }

    @Override
    public PoolEntity toDepositEntity(Pool deposit) {
        if ( deposit == null ) {
            return null;
        }

        PoolEntity.PoolEntityBuilder<?, ?> poolEntity = PoolEntity.builder();

        poolEntity.blockNumber( deposit.getBlockNumber() );
        poolEntity.blockTime( deposit.getBlockTime() );
        poolEntity.activeEpoch( deposit.getActiveEpoch() );
        poolEntity.amount( deposit.getAmount() );
        poolEntity.blockHash( deposit.getBlockHash() );
        poolEntity.certIndex( deposit.getCertIndex() );
        poolEntity.epoch( deposit.getEpoch() );
        poolEntity.poolId( deposit.getPoolId() );
        poolEntity.registrationSlot( deposit.getRegistrationSlot() );
        poolEntity.retireEpoch( deposit.getRetireEpoch() );
        poolEntity.slot( deposit.getSlot() );
        poolEntity.status( deposit.getStatus() );
        poolEntity.txHash( deposit.getTxHash() );
        poolEntity.txIndex( deposit.getTxIndex() );

        return poolEntity.build();
    }
}

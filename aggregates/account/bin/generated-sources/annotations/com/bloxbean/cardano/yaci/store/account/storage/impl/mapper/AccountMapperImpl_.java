package com.bloxbean.cardano.yaci.store.account.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.account.domain.AddressBalance;
import com.bloxbean.cardano.yaci.store.account.domain.StakeAddressBalance;
import com.bloxbean.cardano.yaci.store.account.storage.impl.model.AddressBalanceEntity;
import com.bloxbean.cardano.yaci.store.account.storage.impl.model.StakeAddressBalanceEntity;
import javax.annotation.processing.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-20T15:09:26+0100",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.v20250514-1000, environment: Java 21.0.7 (Eclipse Adoptium)"
)
public class AccountMapperImpl_ implements AccountMapper {

    @Override
    public AddressBalance toAddressBalance(AddressBalanceEntity entity) {
        if ( entity == null ) {
            return null;
        }

        AddressBalance.AddressBalanceBuilder<?, ?> addressBalance = AddressBalance.builder();

        addressBalance.blockNumber( entity.getBlockNumber() );
        addressBalance.blockTime( entity.getBlockTime() );
        addressBalance.address( entity.getAddress() );
        addressBalance.epoch( entity.getEpoch() );
        addressBalance.quantity( entity.getQuantity() );
        addressBalance.slot( entity.getSlot() );
        addressBalance.unit( entity.getUnit() );

        return addressBalance.build();
    }

    @Override
    public AddressBalanceEntity toAddressBalanceEntity(AddressBalance addressBalance) {
        if ( addressBalance == null ) {
            return null;
        }

        AddressBalanceEntity.AddressBalanceEntityBuilder<?, ?> addressBalanceEntity = AddressBalanceEntity.builder();

        addressBalanceEntity.blockNumber( addressBalance.getBlockNumber() );
        addressBalanceEntity.blockTime( addressBalance.getBlockTime() );
        addressBalanceEntity.address( addressBalance.getAddress() );
        addressBalanceEntity.epoch( addressBalance.getEpoch() );
        addressBalanceEntity.quantity( addressBalance.getQuantity() );
        addressBalanceEntity.slot( addressBalance.getSlot() );
        addressBalanceEntity.unit( addressBalance.getUnit() );

        return addressBalanceEntity.build();
    }

    @Override
    public StakeAddressBalance toStakeBalance(StakeAddressBalanceEntity entity) {
        if ( entity == null ) {
            return null;
        }

        StakeAddressBalance.StakeAddressBalanceBuilder<?, ?> stakeAddressBalance = StakeAddressBalance.builder();

        stakeAddressBalance.blockNumber( entity.getBlockNumber() );
        stakeAddressBalance.blockTime( entity.getBlockTime() );
        stakeAddressBalance.address( entity.getAddress() );
        stakeAddressBalance.epoch( entity.getEpoch() );
        stakeAddressBalance.quantity( entity.getQuantity() );
        stakeAddressBalance.slot( entity.getSlot() );

        return stakeAddressBalance.build();
    }

    @Override
    public StakeAddressBalanceEntity toStakeBalanceEntity(StakeAddressBalance stakeBalance) {
        if ( stakeBalance == null ) {
            return null;
        }

        StakeAddressBalanceEntity.StakeAddressBalanceEntityBuilder<?, ?> stakeAddressBalanceEntity = StakeAddressBalanceEntity.builder();

        stakeAddressBalanceEntity.blockNumber( stakeBalance.getBlockNumber() );
        stakeAddressBalanceEntity.blockTime( stakeBalance.getBlockTime() );
        stakeAddressBalanceEntity.address( stakeBalance.getAddress() );
        stakeAddressBalanceEntity.epoch( stakeBalance.getEpoch() );
        stakeAddressBalanceEntity.quantity( stakeBalance.getQuantity() );
        stakeAddressBalanceEntity.slot( stakeBalance.getSlot() );

        return stakeAddressBalanceEntity.build();
    }
}

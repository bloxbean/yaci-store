package com.bloxbean.cardano.yaci.store.account.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.account.domain.AddressTxAmount;
import com.bloxbean.cardano.yaci.store.account.storage.impl.model.AddressTxAmountEntity;
import javax.annotation.processing.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-20T15:09:26+0100",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.v20250514-1000, environment: Java 21.0.7 (Eclipse Adoptium)"
)
public class AggrMapperImpl_ implements AggrMapper {

    @Override
    public AddressTxAmount toAddressTxAmount(AddressTxAmountEntity entity) {
        if ( entity == null ) {
            return null;
        }

        AddressTxAmount.AddressTxAmountBuilder<?, ?> addressTxAmount = AddressTxAmount.builder();

        addressTxAmount.blockNumber( entity.getBlockNumber() );
        addressTxAmount.blockTime( entity.getBlockTime() );
        addressTxAmount.address( entity.getAddress() );
        addressTxAmount.epoch( entity.getEpoch() );
        addressTxAmount.quantity( entity.getQuantity() );
        addressTxAmount.slot( entity.getSlot() );
        addressTxAmount.stakeAddress( entity.getStakeAddress() );
        addressTxAmount.txHash( entity.getTxHash() );
        addressTxAmount.unit( entity.getUnit() );

        return addressTxAmount.build();
    }

    @Override
    public AddressTxAmountEntity toAddressTxAmountEntity(AddressTxAmount addressTxAmount) {
        if ( addressTxAmount == null ) {
            return null;
        }

        AddressTxAmountEntity.AddressTxAmountEntityBuilder addressTxAmountEntity = AddressTxAmountEntity.builder();

        addressTxAmountEntity.address( addressTxAmount.getAddress() );
        addressTxAmountEntity.blockNumber( addressTxAmount.getBlockNumber() );
        addressTxAmountEntity.blockTime( addressTxAmount.getBlockTime() );
        addressTxAmountEntity.epoch( addressTxAmount.getEpoch() );
        addressTxAmountEntity.quantity( addressTxAmount.getQuantity() );
        addressTxAmountEntity.slot( addressTxAmount.getSlot() );
        addressTxAmountEntity.stakeAddress( addressTxAmount.getStakeAddress() );
        addressTxAmountEntity.txHash( addressTxAmount.getTxHash() );
        addressTxAmountEntity.unit( addressTxAmount.getUnit() );

        return addressTxAmountEntity.build();
    }
}

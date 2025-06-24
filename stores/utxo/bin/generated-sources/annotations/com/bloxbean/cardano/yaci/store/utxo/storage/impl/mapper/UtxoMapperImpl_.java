package com.bloxbean.cardano.yaci.store.utxo.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.common.domain.TxInput;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.model.AddressUtxoEntity;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.model.TxInputEntity;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-20T15:09:25+0100",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.v20250514-1000, environment: Java 21.0.7 (Eclipse Adoptium)"
)
public class UtxoMapperImpl_ implements UtxoMapper {

    @Override
    public AddressUtxoEntity toAddressUtxoEntity(AddressUtxo addressUtxo) {
        if ( addressUtxo == null ) {
            return null;
        }

        AddressUtxoEntity.AddressUtxoEntityBuilder<?, ?> addressUtxoEntity = AddressUtxoEntity.builder();

        addressUtxoEntity.blockNumber( addressUtxo.getBlockNumber() );
        addressUtxoEntity.blockTime( addressUtxo.getBlockTime() );
        List<Amt> list = addressUtxo.getAmounts();
        if ( list != null ) {
            addressUtxoEntity.amounts( new ArrayList<Amt>( list ) );
        }
        addressUtxoEntity.blockHash( addressUtxo.getBlockHash() );
        addressUtxoEntity.dataHash( addressUtxo.getDataHash() );
        addressUtxoEntity.epoch( addressUtxo.getEpoch() );
        addressUtxoEntity.inlineDatum( addressUtxo.getInlineDatum() );
        addressUtxoEntity.isCollateralReturn( addressUtxo.getIsCollateralReturn() );
        addressUtxoEntity.lovelaceAmount( addressUtxo.getLovelaceAmount() );
        addressUtxoEntity.outputIndex( addressUtxo.getOutputIndex() );
        addressUtxoEntity.ownerAddr( addressUtxo.getOwnerAddr() );
        addressUtxoEntity.ownerPaymentCredential( addressUtxo.getOwnerPaymentCredential() );
        addressUtxoEntity.ownerStakeAddr( addressUtxo.getOwnerStakeAddr() );
        addressUtxoEntity.ownerStakeCredential( addressUtxo.getOwnerStakeCredential() );
        addressUtxoEntity.referenceScriptHash( addressUtxo.getReferenceScriptHash() );
        addressUtxoEntity.scriptRef( addressUtxo.getScriptRef() );
        addressUtxoEntity.slot( addressUtxo.getSlot() );
        addressUtxoEntity.txHash( addressUtxo.getTxHash() );

        return addressUtxoEntity.build();
    }

    @Override
    public AddressUtxo toAddressUtxo(AddressUtxoEntity entity) {
        if ( entity == null ) {
            return null;
        }

        AddressUtxo.AddressUtxoBuilder<?, ?> addressUtxo = AddressUtxo.builder();

        addressUtxo.blockNumber( entity.getBlockNumber() );
        addressUtxo.blockTime( entity.getBlockTime() );
        List<Amt> list = entity.getAmounts();
        if ( list != null ) {
            addressUtxo.amounts( new ArrayList<Amt>( list ) );
        }
        addressUtxo.blockHash( entity.getBlockHash() );
        addressUtxo.dataHash( entity.getDataHash() );
        addressUtxo.epoch( entity.getEpoch() );
        addressUtxo.inlineDatum( entity.getInlineDatum() );
        addressUtxo.isCollateralReturn( entity.getIsCollateralReturn() );
        addressUtxo.lovelaceAmount( entity.getLovelaceAmount() );
        addressUtxo.outputIndex( entity.getOutputIndex() );
        addressUtxo.ownerAddr( entity.getOwnerAddr() );
        addressUtxo.ownerPaymentCredential( entity.getOwnerPaymentCredential() );
        addressUtxo.ownerStakeAddr( entity.getOwnerStakeAddr() );
        addressUtxo.ownerStakeCredential( entity.getOwnerStakeCredential() );
        addressUtxo.referenceScriptHash( entity.getReferenceScriptHash() );
        addressUtxo.scriptRef( entity.getScriptRef() );
        addressUtxo.slot( entity.getSlot() );
        addressUtxo.txHash( entity.getTxHash() );

        return addressUtxo.build();
    }

    @Override
    public TxInput toTxInput(TxInputEntity txInputEntity) {
        if ( txInputEntity == null ) {
            return null;
        }

        TxInput.TxInputBuilder<?, ?> txInput = TxInput.builder();

        txInput.outputIndex( txInputEntity.getOutputIndex() );
        txInput.spentAtBlock( txInputEntity.getSpentAtBlock() );
        txInput.spentAtBlockHash( txInputEntity.getSpentAtBlockHash() );
        txInput.spentAtSlot( txInputEntity.getSpentAtSlot() );
        txInput.spentBlockTime( txInputEntity.getSpentBlockTime() );
        txInput.spentEpoch( txInputEntity.getSpentEpoch() );
        txInput.spentTxHash( txInputEntity.getSpentTxHash() );
        txInput.txHash( txInputEntity.getTxHash() );

        return txInput.build();
    }
}

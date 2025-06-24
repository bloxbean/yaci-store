package com.bloxbean.cardano.yaci.store.transaction.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import com.bloxbean.cardano.yaci.store.transaction.domain.InvalidTransaction;
import com.bloxbean.cardano.yaci.store.transaction.domain.Txn;
import com.bloxbean.cardano.yaci.store.transaction.domain.TxnWitness;
import com.bloxbean.cardano.yaci.store.transaction.domain.Withdrawal;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.model.InvalidTransactionEntity;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.model.TxnEntity;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.model.TxnWitnessEntity;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.model.WithdrawalEntity;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-20T15:09:24+0100",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.v20250514-1000, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class TxnMapperImpl extends TxnMapper {

    @Override
    public TxnEntity toTxnEntity(Txn txn) {
        if ( txn == null ) {
            return null;
        }

        TxnEntity.TxnEntityBuilder<?, ?> txnEntity = TxnEntity.builder();

        txnEntity.blockNumber( txn.getBlockNumber() );
        txnEntity.blockTime( txn.getBlockTime() );
        txnEntity.auxiliaryDataHash( txn.getAuxiliaryDataHash() );
        txnEntity.blockHash( txn.getBlockHash() );
        List<UtxoKey> list = txn.getCollateralInputs();
        if ( list != null ) {
            txnEntity.collateralInputs( new ArrayList<UtxoKey>( list ) );
        }
        txnEntity.collateralReturn( txn.getCollateralReturn() );
        txnEntity.collateralReturnJson( txn.getCollateralReturnJson() );
        txnEntity.epoch( txn.getEpoch() );
        txnEntity.fee( txn.getFee() );
        List<UtxoKey> list1 = txn.getInputs();
        if ( list1 != null ) {
            txnEntity.inputs( new ArrayList<UtxoKey>( list1 ) );
        }
        txnEntity.invalid( txn.getInvalid() );
        txnEntity.netowrkId( txn.getNetowrkId() );
        List<UtxoKey> list2 = txn.getOutputs();
        if ( list2 != null ) {
            txnEntity.outputs( new ArrayList<UtxoKey>( list2 ) );
        }
        List<UtxoKey> list3 = txn.getReferenceInputs();
        if ( list3 != null ) {
            txnEntity.referenceInputs( new ArrayList<UtxoKey>( list3 ) );
        }
        Set<String> set = txn.getRequiredSigners();
        if ( set != null ) {
            txnEntity.requiredSigners( new LinkedHashSet<String>( set ) );
        }
        txnEntity.scriptDataHash( txn.getScriptDataHash() );
        txnEntity.slot( txn.getSlot() );
        txnEntity.totalCollateral( txn.getTotalCollateral() );
        txnEntity.treasuryDonation( txn.getTreasuryDonation() );
        txnEntity.ttl( txn.getTtl() );
        txnEntity.txHash( txn.getTxHash() );
        txnEntity.txIndex( txn.getTxIndex() );
        txnEntity.validityIntervalStart( txn.getValidityIntervalStart() );

        return txnEntity.build();
    }

    @Override
    public Txn toTxn(TxnEntity entity) {
        if ( entity == null ) {
            return null;
        }

        Txn.TxnBuilder<?, ?> txn = Txn.builder();

        txn.blockNumber( entity.getBlockNumber() );
        txn.blockTime( entity.getBlockTime() );
        txn.auxiliaryDataHash( entity.getAuxiliaryDataHash() );
        txn.blockHash( entity.getBlockHash() );
        List<UtxoKey> list = entity.getCollateralInputs();
        if ( list != null ) {
            txn.collateralInputs( new ArrayList<UtxoKey>( list ) );
        }
        txn.collateralReturn( entity.getCollateralReturn() );
        txn.collateralReturnJson( entity.getCollateralReturnJson() );
        txn.epoch( entity.getEpoch() );
        txn.fee( entity.getFee() );
        List<UtxoKey> list1 = entity.getInputs();
        if ( list1 != null ) {
            txn.inputs( new ArrayList<UtxoKey>( list1 ) );
        }
        txn.invalid( entity.getInvalid() );
        txn.netowrkId( entity.getNetowrkId() );
        List<UtxoKey> list2 = entity.getOutputs();
        if ( list2 != null ) {
            txn.outputs( new ArrayList<UtxoKey>( list2 ) );
        }
        List<UtxoKey> list3 = entity.getReferenceInputs();
        if ( list3 != null ) {
            txn.referenceInputs( new ArrayList<UtxoKey>( list3 ) );
        }
        Set<String> set = entity.getRequiredSigners();
        if ( set != null ) {
            txn.requiredSigners( new LinkedHashSet<String>( set ) );
        }
        txn.scriptDataHash( entity.getScriptDataHash() );
        txn.slot( entity.getSlot() );
        txn.totalCollateral( entity.getTotalCollateral() );
        txn.treasuryDonation( entity.getTreasuryDonation() );
        txn.ttl( entity.getTtl() );
        txn.txHash( entity.getTxHash() );
        txn.txIndex( entity.getTxIndex() );
        txn.validityIntervalStart( entity.getValidityIntervalStart() );

        return txn.build();
    }

    @Override
    public TxnWitness toTxnWitness(TxnWitnessEntity entity) {
        if ( entity == null ) {
            return null;
        }

        TxnWitness.TxnWitnessBuilder<?, ?> txnWitness = TxnWitness.builder();

        txnWitness.additionalData( entity.getAdditionalData() );
        txnWitness.index( entity.getIndex() );
        txnWitness.pubKey( entity.getPubKey() );
        txnWitness.pubKeyhash( entity.getPubKeyhash() );
        txnWitness.signature( entity.getSignature() );
        txnWitness.slot( entity.getSlot() );
        txnWitness.txHash( entity.getTxHash() );
        txnWitness.type( entity.getType() );

        return txnWitness.build();
    }

    @Override
    public TxnWitnessEntity toTxnWitnessEntity(TxnWitness txnWitness) {
        if ( txnWitness == null ) {
            return null;
        }

        TxnWitnessEntity.TxnWitnessEntityBuilder<?, ?> txnWitnessEntity = TxnWitnessEntity.builder();

        txnWitnessEntity.additionalData( txnWitness.getAdditionalData() );
        txnWitnessEntity.index( txnWitness.getIndex() );
        txnWitnessEntity.pubKey( txnWitness.getPubKey() );
        txnWitnessEntity.pubKeyhash( txnWitness.getPubKeyhash() );
        txnWitnessEntity.signature( txnWitness.getSignature() );
        txnWitnessEntity.slot( txnWitness.getSlot() );
        txnWitnessEntity.txHash( txnWitness.getTxHash() );
        txnWitnessEntity.type( txnWitness.getType() );

        return txnWitnessEntity.build();
    }

    @Override
    public Withdrawal toWithdrawal(WithdrawalEntity withdrawalEntity) {
        if ( withdrawalEntity == null ) {
            return null;
        }

        Withdrawal.WithdrawalBuilder<?, ?> withdrawal = Withdrawal.builder();

        withdrawal.blockNumber( withdrawalEntity.getBlockNumber() );
        withdrawal.blockTime( withdrawalEntity.getBlockTime() );
        withdrawal.address( withdrawalEntity.getAddress() );
        withdrawal.amount( withdrawalEntity.getAmount() );
        withdrawal.epoch( withdrawalEntity.getEpoch() );
        withdrawal.slot( withdrawalEntity.getSlot() );
        withdrawal.txHash( withdrawalEntity.getTxHash() );

        return withdrawal.build();
    }

    @Override
    public WithdrawalEntity toWithdrawalEntity(Withdrawal withdrawal) {
        if ( withdrawal == null ) {
            return null;
        }

        WithdrawalEntity.WithdrawalEntityBuilder<?, ?> withdrawalEntity = WithdrawalEntity.builder();

        withdrawalEntity.blockNumber( withdrawal.getBlockNumber() );
        withdrawalEntity.blockTime( withdrawal.getBlockTime() );
        withdrawalEntity.address( withdrawal.getAddress() );
        withdrawalEntity.amount( withdrawal.getAmount() );
        withdrawalEntity.epoch( withdrawal.getEpoch() );
        withdrawalEntity.slot( withdrawal.getSlot() );
        withdrawalEntity.txHash( withdrawal.getTxHash() );

        return withdrawalEntity.build();
    }

    @Override
    public InvalidTransactionEntity toInvalidTransactionEntity(InvalidTransaction invalidTransaction) {
        if ( invalidTransaction == null ) {
            return null;
        }

        InvalidTransactionEntity.InvalidTransactionEntityBuilder invalidTransactionEntity = InvalidTransactionEntity.builder();

        invalidTransactionEntity.blockHash( invalidTransaction.getBlockHash() );
        invalidTransactionEntity.slot( invalidTransaction.getSlot() );
        invalidTransactionEntity.transaction( invalidTransaction.getTransaction() );
        invalidTransactionEntity.txHash( invalidTransaction.getTxHash() );

        return invalidTransactionEntity.build();
    }

    @Override
    public InvalidTransaction toInvalidTransaction(InvalidTransactionEntity invalidTransactionEntity) {
        if ( invalidTransactionEntity == null ) {
            return null;
        }

        InvalidTransaction.InvalidTransactionBuilder invalidTransaction = InvalidTransaction.builder();

        invalidTransaction.blockHash( invalidTransactionEntity.getBlockHash() );
        invalidTransaction.slot( invalidTransactionEntity.getSlot() );
        invalidTransaction.transaction( invalidTransactionEntity.getTransaction() );
        invalidTransaction.txHash( invalidTransactionEntity.getTxHash() );

        return invalidTransaction.build();
    }
}

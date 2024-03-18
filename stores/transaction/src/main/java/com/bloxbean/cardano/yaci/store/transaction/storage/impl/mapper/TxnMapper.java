package com.bloxbean.cardano.yaci.store.transaction.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.transaction.domain.InvalidTransaction;
import com.bloxbean.cardano.yaci.store.transaction.domain.Txn;
import com.bloxbean.cardano.yaci.store.transaction.domain.TxnWitness;
import com.bloxbean.cardano.yaci.store.transaction.domain.Withdrawal;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.model.*;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.model.TxnEntityJpa;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.model.WithdrawalEntityJpa;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class TxnMapper {
    public abstract TxnEntityJpa toTxnEntity(Txn txn);
    public abstract Txn toTxn(TxnEntityJpa entity);

    public abstract TxnWitness toTxnWitness(TxnWitnessEntity entity);
    public abstract TxnWitnessEntity toTxnWitnessEntity(TxnWitness txnWitness);

    public abstract Withdrawal toWithdrawal(WithdrawalEntityJpa withdrawalEntity);
    public abstract WithdrawalEntityJpa toWithdrawalEntity(Withdrawal withdrawal);

    public abstract InvalidTransactionEntity toInvalidTransactionEntity(InvalidTransaction invalidTransaction);
    public abstract InvalidTransaction toInvalidTransaction(InvalidTransactionEntity invalidTransactionEntity);
}

package com.bloxbean.cardano.yaci.store.cip139.transaction.service;

import com.bloxbean.cardano.yaci.store.cip139.transaction.dto.TransactionDto;
import com.bloxbean.cardano.yaci.store.transaction.domain.Txn;
import com.bloxbean.cardano.yaci.store.transaction.domain.TxnWitness;
import com.bloxbean.cardano.yaci.store.transaction.storage.TransactionStorageReader;
import com.bloxbean.cardano.yaci.store.transaction.storage.TransactionWitnessStorageReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class Cip139TransactionService {

    private final TransactionStorageReader transactionStorageReader;
    private final TransactionWitnessStorageReader transactionWitnessStorageReader;

    public Optional<TransactionDto> getTransactionByHash(String hash) {

        Optional<Txn> optionalDomainTransactionForHash = transactionStorageReader.getTransactionByTxHash(hash);
        List<TxnWitness> transactionWitnesses = transactionWitnessStorageReader.getTransactionWitnesses(hash);

        TransactionDto transactionDto;

        if (optionalDomainTransactionForHash.isPresent()) {
            Txn domainTransaction = optionalDomainTransactionForHash.get();
            transactionDto = TransactionDto.fromDomain(domainTransaction, transactionWitnesses);
            return Optional.of(transactionDto);
        } else {
            return Optional.empty();
        }
    }
}

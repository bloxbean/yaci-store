package com.bloxbean.cardano.yaci.store.transaction.storage.impl;

import com.bloxbean.cardano.yaci.store.transaction.domain.TxnCbor;
import com.bloxbean.cardano.yaci.store.transaction.storage.TransactionCborStorageReader;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.repository.TxnCborRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
public class TransactionCborStorageReaderImpl implements TransactionCborStorageReader {
    private final TxnCborRepository txnCborRepository;

    @Override
    public Optional<TxnCbor> getTxCborByHash(String txHash) {
        return txnCborRepository.findByTxHash(txHash)
                .map(entity -> TxnCbor.builder()
                        .txHash(entity.getTxHash())
                        .cborData(entity.getCborData())
                        .cborSize(entity.getCborSize())
                        .slot(entity.getSlot())
                        .build());
    }

    @Override
    public boolean cborExists(String txHash) {
        return txnCborRepository.findByTxHash(txHash).isPresent();
    }
}


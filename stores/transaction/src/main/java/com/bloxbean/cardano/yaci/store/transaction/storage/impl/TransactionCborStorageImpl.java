package com.bloxbean.cardano.yaci.store.transaction.storage.impl;

import com.bloxbean.cardano.yaci.store.transaction.domain.Txn;
import com.bloxbean.cardano.yaci.store.transaction.storage.TransactionCborStorage;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.model.TxnCborEntity;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.repository.TxnCborRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;

import java.util.List;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.yaci.store.transaction.jooq.Tables.TRANSACTION_CBOR;

/**
 * Implementation of TransactionCborStorage for managing transaction CBOR data
 */
@RequiredArgsConstructor
@Slf4j
public class TransactionCborStorageImpl implements TransactionCborStorage {
    private final TxnCborRepository txnCborRepository;
    private final DSLContext dsl;

    @Override
    public void saveAll(List<Txn> txnList) {
        List<TxnCborEntity> cborEntities = txnList.stream()
                .filter(txn -> txn.getTxBodyCbor() != null && txn.getTxBodyCbor().length > 0)
                .map(txn -> TxnCborEntity.builder()
                        .txHash(txn.getTxHash())
                        .cborData(txn.getTxBodyCbor())
                        .cborSize(txn.getTxBodyCbor().length)
                        .slot(txn.getSlot())
                        .build())
                .collect(Collectors.toList());
        
        if (!cborEntities.isEmpty()) {
            txnCborRepository.saveAll(cborEntities);
            log.debug("Saved CBOR data for {} transactions", cborEntities.size());
        }
    }

    @Override
    public int deleteBySlotGreaterThan(long slot) {
        int deleted = txnCborRepository.deleteBySlotGreaterThan(slot);
        log.debug("Deleted {} transaction CBOR records for rollback (slot > {})", deleted, slot);
        return deleted;
    }

    @Override
    public int deleteBySlotLessThan(long slot) {
        int deleted = dsl.deleteFrom(TRANSACTION_CBOR)
                .where(TRANSACTION_CBOR.SLOT.lessThan(slot))
                .execute();
        log.debug("Deleted {} transaction CBOR records for pruning (slot < {})", deleted, slot);
        return deleted;
    }
}


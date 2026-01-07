package com.bloxbean.cardano.yaci.store.submit.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.submit.domain.SubmittedTransaction;
import com.bloxbean.cardano.yaci.store.submit.domain.TxStatus;
import com.bloxbean.cardano.yaci.store.submit.storage.impl.model.SubmittedTransactionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Set;

@Repository
public interface SubmittedTransactionRepository extends JpaRepository<SubmittedTransactionEntity, String> {

    List<SubmittedTransactionEntity> findByStatusAndConfirmedBlockNumberLessThan(TxStatus status, Long confirmedBlockNumberIsLessThan);

    List<SubmittedTransactionEntity> findByTxHashIn(List<String> txHashes);

    List<SubmittedTransactionEntity> findByConfirmedSlotGreaterThan(Long slot);
}


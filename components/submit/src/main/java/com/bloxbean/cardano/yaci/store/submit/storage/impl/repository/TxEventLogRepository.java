package com.bloxbean.cardano.yaci.store.submit.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.submit.storage.impl.model.TxEventLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TxEventLogRepository extends JpaRepository<TxEventLogEntity, Long> {
}


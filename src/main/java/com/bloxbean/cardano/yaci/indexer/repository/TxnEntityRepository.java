package com.bloxbean.cardano.yaci.indexer.repository;

import com.bloxbean.cardano.yaci.indexer.entity.TxnEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TxnEntityRepository extends JpaRepository<TxnEntity, String> {
}

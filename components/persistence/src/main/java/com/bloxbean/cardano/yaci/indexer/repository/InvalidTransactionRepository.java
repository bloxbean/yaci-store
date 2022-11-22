package com.bloxbean.cardano.yaci.indexer.repository;

import com.bloxbean.cardano.yaci.indexer.entity.InvalidTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvalidTransactionRepository extends JpaRepository<InvalidTransaction, String> {

}


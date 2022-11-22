package com.bloxbean.cardano.yaci.indexer.repository;

import com.bloxbean.cardano.yaci.indexer.entity.AddressUtxo;
import com.bloxbean.cardano.yaci.indexer.entity.UtxoId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UtxoRepository extends JpaRepository<AddressUtxo, UtxoId> {
}


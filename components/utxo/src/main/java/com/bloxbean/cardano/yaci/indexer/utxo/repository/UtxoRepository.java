package com.bloxbean.cardano.yaci.indexer.utxo.repository;

import com.bloxbean.cardano.yaci.indexer.utxo.entity.AddressUtxo;
import com.bloxbean.cardano.yaci.indexer.utxo.entity.UtxoId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UtxoRepository extends JpaRepository<AddressUtxo, UtxoId> {

    Optional<List<AddressUtxo>> findAddressUtxoByOwnerAddrAndSpent(String ownerAddress, boolean spent, Pageable page);

    List<AddressUtxo> findAllById(Iterable<UtxoId> utxoIds);
}


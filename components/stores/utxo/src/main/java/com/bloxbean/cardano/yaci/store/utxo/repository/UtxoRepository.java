package com.bloxbean.cardano.yaci.store.utxo.repository;

import com.bloxbean.cardano.yaci.store.utxo.model.AddressUtxoEntity;
import com.bloxbean.cardano.yaci.store.utxo.model.UtxoId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UtxoRepository extends JpaRepository<AddressUtxoEntity, UtxoId> {

    Optional<List<AddressUtxoEntity>> findAddressUtxoByOwnerAddrAndSpent(String ownerAddress, Boolean spent, Pageable page);

    List<AddressUtxoEntity> findAllById(Iterable<UtxoId> utxoIds);

//    @Modifying
//    @Query(value = "DELETE FROM AddressUtxo a WHERE a.slot > :slot")
//    int deleteBySlotAfter(@Param("slot") long slot);

    List<AddressUtxoEntity> findBySlot(Long slot);

    int deleteBySlotGreaterThan(Long slot);
}


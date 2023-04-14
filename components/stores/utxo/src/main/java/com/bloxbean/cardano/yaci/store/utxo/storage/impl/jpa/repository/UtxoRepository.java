package com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.repository;

import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.model.AddressUtxoEntity;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.model.UtxoId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UtxoRepository extends JpaRepository<AddressUtxoEntity, UtxoId> {
    Optional<List<AddressUtxoEntity>> findAddressUtxoByOwnerAddrAndSpent(String ownerAddress, Boolean spent, Pageable page);

    Optional<List<AddressUtxoEntity>> findByOwnerPaymentCredentialAndSpent(String paymentKeyHash, Boolean spent, Pageable page);

    List<AddressUtxoEntity> findAllById(Iterable<UtxoId> utxoIds);

    List<AddressUtxoEntity> findBySlot(Long slot);

    int deleteBySlotGreaterThan(Long slot);
}


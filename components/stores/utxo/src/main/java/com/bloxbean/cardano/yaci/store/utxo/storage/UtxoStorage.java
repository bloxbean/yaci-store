package com.bloxbean.cardano.yaci.store.utxo.storage;

import com.bloxbean.cardano.yaci.store.utxo.domain.AddressUtxo;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface UtxoStorage {
    Optional<AddressUtxo> findById(String txHash, int outputIndex);
    Optional<List<AddressUtxo>> findAddressUtxoByOwnerAddrAndSpent(String ownerAddress, Boolean spent, Pageable page);
    List<AddressUtxo> findBySlot(Long slot);
    int deleteBySlotGreaterThan(Long slot);

    Optional<AddressUtxo> save(AddressUtxo addressUtxo);
    Optional<List<AddressUtxo>> saveAll(List<AddressUtxo> addressUtxoList);
}

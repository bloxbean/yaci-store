package com.bloxbean.cardano.yaci.store.utxo.storage.api;

import com.bloxbean.carano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.carano.yaci.store.common.domain.UtxoKey;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface UtxoStorage {
    Optional<AddressUtxo> findById(String txHash, int outputIndex);
    Optional<List<AddressUtxo>> findAddressUtxoByOwnerAddrAndSpent(String ownerAddress, Boolean spent, Pageable page);
    List<AddressUtxo> findBySlot(Long slot);
    List<AddressUtxo> findAllByIds(List<UtxoKey> utxoKeys);
    int deleteBySlotGreaterThan(Long slot);

    Optional<AddressUtxo> save(AddressUtxo addressUtxo);
    Optional<List<AddressUtxo>> saveAll(List<AddressUtxo> addressUtxoList);
}

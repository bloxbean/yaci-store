package com.bloxbean.cardano.yaci.store.utxo.storage.api;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.utxo.domain.AssetHolder;

import java.util.List;
import java.util.Optional;

public interface UtxoStorage {
    Optional<AddressUtxo> findById(String txHash, int outputIndex);
    Optional<List<AddressUtxo>> findUtxoByAddress(String address, int page, int count, Order order);
    Optional<List<AddressUtxo>> findUtxoByAddressAndAsset(String ownerAddress, String unit, int page, int count, Order order);
    Optional<List<AddressUtxo>> findUtxoByAddressAndSpent(String address, Boolean spent, int page, int count, Order order);

    Optional<List<AddressUtxo>> findUtxoByPaymentCredential(String paymentCredential, int page, int count, Order order);
    Optional<List<AddressUtxo>> findUtxoByPaymentCredentialAndAsset(String paymentCredential, String unit, int page, int count, Order order);
    Optional<List<AddressUtxo>> findUtxoByPaymentCredentialAndSpent(String paymentCredential, Boolean spent, int page, int count, Order order);

    Optional<List<AddressUtxo>> findUtxoByStakeAddress(String stakeAddress, int page, int count, Order order);
    Optional<List<AddressUtxo>> findUtxoByStakeAddressAndAsset(String stakeAddress, String unit, int page, int count, Order order);

    List<AddressUtxo> findBySlot(Long slot);
    List<AddressUtxo> findAllByIds(List<UtxoKey> utxoKeys);
    int deleteBySlotGreaterThan(Long slot);

    Optional<AddressUtxo> save(AddressUtxo addressUtxo);
    Optional<List<AddressUtxo>> saveAll(List<AddressUtxo> addressUtxoList);

    List<AssetHolder> findAssetHoldersByUnit(String unit, int page, int count);
}

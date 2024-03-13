package com.bloxbean.cardano.yaci.store.utxo.storage;

import com.bloxbean.cardano.yaci.core.util.Tuple;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.TxInput;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import com.bloxbean.cardano.yaci.store.common.model.Order;

import java.util.List;
import java.util.Optional;

public interface UtxoStorageReader {

    Optional<AddressUtxo> findById(String txHash, int outputIndex);
    List<AddressUtxo> findUtxoByAddress(String address, int page, int count, Order order);
    List<AddressUtxo> findUtxosByAsset(String unit, int page, int count, Order order);
    List<AddressUtxo> findUtxoByAddressAndAsset(String ownerAddress, String unit, int page, int count, Order order);
    List<AddressUtxo> findUtxoByPaymentCredential(String paymentCredential, int page, int count, Order order);
    List<AddressUtxo> findUtxoByPaymentCredentialAndAsset(String paymentCredential, String unit, int page, int count, Order order);
    List<AddressUtxo> findUtxoByStakeAddress(String stakeAddress, int page, int count, Order order);
    List<AddressUtxo> findUtxoByStakeAddressAndAsset(String stakeAddress, String unit, int page, int count, Order order);
    List<AddressUtxo> findAllByIds(List<UtxoKey> utxoKeys);
    List<Long> findNextAvailableBlocks(Long block, int limit);
    List<AddressUtxo> findUnspentUtxosBetweenBlocks(Long startBlock, Long endBlock);
    List<Tuple<AddressUtxo, TxInput>> findSpentUtxosBetweenBlocks(Long startBlock, Long endBlock);
}

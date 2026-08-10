package com.bloxbean.cardano.yaci.store.blockfrost.address.storage;

import com.bloxbean.cardano.yaci.store.blockfrost.address.dto.BFAddressTransactionDTO;
import com.bloxbean.cardano.yaci.store.blockfrost.address.storage.impl.model.BFAddressTotal;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.model.Order;

import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.math.BigInteger;

public interface BFAddressStorageReader {
    List<String> findTxHashesByAddress(String address, int page, int count, Order order);

    List<BFAddressTransactionDTO> findAddressTransactions(String address, int page, int count, Order order, String from, String to);

    /**
     * Unspent UTXOs for an address in Blockfrost-compatible on-chain order (slot, tx_index, output_index).
     */
    List<AddressUtxo> findAddressUtxos(String address, int page, int count, Order order);

    /**
     * Unspent UTXOs for an address holding a given asset unit, in Blockfrost-compatible on-chain order.
     */
    List<AddressUtxo> findAddressUtxosForAsset(String address, String unit, int page, int count, Order order);

    Optional<BFAddressTotal> getAddressTotal(String address);

    Map<String, BigInteger> findCurrentAddressBalanceByUnit(String address);

    Map<String, BigInteger> findUnspentAddressBalanceByUnit(String address);
}

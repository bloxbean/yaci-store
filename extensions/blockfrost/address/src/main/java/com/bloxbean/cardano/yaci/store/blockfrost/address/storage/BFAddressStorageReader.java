package com.bloxbean.cardano.yaci.store.blockfrost.address.storage;

import com.bloxbean.cardano.yaci.store.blockfrost.address.dto.BFAddressTransactionDTO;
import com.bloxbean.cardano.yaci.store.blockfrost.address.storage.impl.model.BFAddressTotal;
import com.bloxbean.cardano.yaci.store.common.model.Order;

import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.math.BigInteger;

public interface BFAddressStorageReader {
    List<String> findTxHashesByAddress(String address, int page, int count, Order order);

    List<BFAddressTransactionDTO> findAddressTransactions(String address, int page, int count, Order order, String from, String to);

    Optional<BFAddressTotal> getAddressTotal(String address);

    Map<String, BigInteger> findCurrentAddressBalanceByUnit(String address);

    Map<String, BigInteger> findUnspentAddressBalanceByUnit(String address);
}

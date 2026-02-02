package com.bloxbean.cardano.yaci.store.blockfrost.address.storage;

import com.bloxbean.cardano.yaci.store.blockfrost.address.dto.BFAddressTransactionDTO;
import com.bloxbean.cardano.yaci.store.blockfrost.address.storage.impl.model.BFAddressTotal;
import com.bloxbean.cardano.yaci.store.common.model.Order;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface BFAddressStorageReader {
    List<String> findTxHashesByAddress(String address, int page, int count, Order order);

    List<BFAddressTransactionDTO> findAddressTransactions(String address, int page, int count, Order order, String from, String to);

    Optional<BFAddressTotal> getAddressTotal(String address);

    Map<String, BigInteger> findLatestAddressBalanceByUnit(String address);
}

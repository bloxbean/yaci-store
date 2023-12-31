package com.bloxbean.cardano.yaci.store.extensions.utxo.rocksdb;

import com.bloxbean.cardano.yaci.core.util.Tuple;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.TxInput;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorageReader;
import com.bloxbean.rocks.types.collection.RocksMap;
import com.bloxbean.rocks.types.collection.RocksMultiZSet;
import com.bloxbean.rocks.types.collection.RocksZSet;
import com.bloxbean.rocks.types.config.RocksDBConfig;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.bloxbean.cardano.yaci.store.extensions.utxo.rocksdb.UtxoStoreKeyUtils.getKey;

@Slf4j
public class RocksDBUtxoStorageReader implements UtxoStorageReader {
    private static final String UTXOS_COL_FAMILY = "utxos";
    private static final String SPENT_UTXOS_COL_FAMILY = "spent_utxos";

    private RocksDBConfig rocksDBConfig;

    private RocksMap<String, AddressUtxo> utxoMap;
    private RocksMap<String, TxInput> spentUtxoMap;
    private RocksZSet<String> utxoSlotZSet;
    private RocksZSet<String> spentUtxoSlotZSet;

    private RocksMultiZSet<String> addressUtxoZSet;
    private RocksMultiZSet<String> paymentCredUtxoZSet;

    public RocksDBUtxoStorageReader(RocksDBConfig rocksDBConfig) {
        this.rocksDBConfig = rocksDBConfig;
        this.utxoMap = new RocksMap<>(rocksDBConfig, UTXOS_COL_FAMILY, String.class, AddressUtxo.class);
        this.spentUtxoMap = new RocksMap<>(rocksDBConfig, SPENT_UTXOS_COL_FAMILY, String.class, TxInput.class);
        this.utxoSlotZSet = new RocksZSet<>(rocksDBConfig, UTXOS_COL_FAMILY, "slot_utxos", String.class);
        this.spentUtxoSlotZSet = new RocksZSet<>(rocksDBConfig, SPENT_UTXOS_COL_FAMILY, "spent_slot_utxos", String.class);

        this.addressUtxoZSet = new RocksMultiZSet<>(rocksDBConfig, UTXOS_COL_FAMILY, "address_utxos", String.class);
        this.paymentCredUtxoZSet = new RocksMultiZSet<>(rocksDBConfig, UTXOS_COL_FAMILY, "payment_cred_utxos", String.class);

        log.info("<< Embedded utxo storage enabled >>");
    }

    @Override
    public Optional<AddressUtxo> findById(String txHash, int outputIndex) {
        return utxoMap.get(getKey(txHash, outputIndex));
    }

    @Override
    public List<AddressUtxo> findAllByIds(List<UtxoKey> utxoKeys) {
        if (utxoKeys.size() == 0)
            return Collections.emptyList();

        return utxoMap.multiGet(utxoKeys.stream()
                .map(utxoKey -> getKey(utxoKey.getTxHash(), utxoKey.getOutputIndex()))
                .toList());
    }

    @SneakyThrows
    @Override
    public List<AddressUtxo> findUtxoByAddress(String address, int page, int count, Order order) {
        byte[] addressNS = UtxoStoreKeyUtils.getAddressBytes(address);
        List<AddressUtxo> utxos = new ArrayList<>();
        try (var iterator = addressUtxoZSet.membersInRangeIterator(addressNS, 0, Long.MAX_VALUE)) {
            iterator.skip(page * count);
            int i = 0;
            while (iterator.hasNext() && i < count) {
                var next = iterator.next();
                utxoMap.get(next._1).ifPresent(utxos::add);
                i++;
            }
        }

        return utxos;
    }

    @Override
    public List<AddressUtxo> findUtxosByAsset(String unit, int page, int count, Order order) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public List<AddressUtxo> findUtxoByAddressAndAsset(String ownerAddress, String unit, int page, int count, Order order) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @SneakyThrows
    @Override
    public List<AddressUtxo> findUtxoByPaymentCredential(String paymentCredential, int page, int count, Order order) {
        byte[] paymentCredNS = UtxoStoreKeyUtils.getPaymentCredential(paymentCredential);
        List<AddressUtxo> utxos = new ArrayList<>();
        try (var iterator = paymentCredUtxoZSet.membersInRangeIterator(paymentCredNS, 0, Long.MAX_VALUE)) {
            iterator.skip(page * count);

            int i = 0;
            while (iterator.hasNext() && i < count) {
                var next = iterator.next();
                utxoMap.get(next._1).ifPresent(utxos::add);
                i++;
            }
        }

        return utxos;
    }

    @Override
    public List<AddressUtxo> findUtxoByPaymentCredentialAndAsset(String paymentCredential, String unit, int page, int count, Order order) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public List<AddressUtxo> findUtxoByStakeAddress(String stakeAddress, int page, int count, Order order) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public List<AddressUtxo> findUtxoByStakeAddressAndAsset(String stakeAddress, String unit, int page, int count, Order order) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public List<Long> findNextAvailableBlocks(Long block, int limit) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public List<AddressUtxo> findUnspentUtxosBetweenBlocks(Long startBlock, Long endBlock) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public List<Tuple<AddressUtxo, TxInput>> findSpentUtxosBetweenBlocks(Long startBlock, Long endBlock) {
        throw new UnsupportedOperationException("Not implemented");
    }

}

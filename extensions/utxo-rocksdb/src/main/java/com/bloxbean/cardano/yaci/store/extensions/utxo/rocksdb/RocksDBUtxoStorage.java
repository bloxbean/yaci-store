package com.bloxbean.cardano.yaci.store.extensions.utxo.rocksdb;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.TxInput;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import com.bloxbean.cardano.yaci.store.common.util.ListUtil;
import com.bloxbean.cardano.yaci.store.events.internal.CommitEvent;
import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorage;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.UtxoCache;
import com.bloxbean.rocks.types.collection.RocksMap;
import com.bloxbean.rocks.types.collection.RocksMultiZSet;
import com.bloxbean.rocks.types.collection.RocksZSet;
import com.bloxbean.rocks.types.collection.util.ValueIterator;
import com.bloxbean.rocks.types.common.Tuple;
import com.bloxbean.rocks.types.config.RocksDBConfig;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.WriteBatch;
import org.rocksdb.WriteOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;

import java.util.*;

import static com.bloxbean.cardano.yaci.store.extensions.utxo.rocksdb.UtxoStoreKeyUtils.*;

@Slf4j
public class RocksDBUtxoStorage implements UtxoStorage {
    private static final String UTXOS_COL_FAMILY = "utxos";
    private static final String SPENT_UTXOS_COL_FAMILY = "spent_utxos";

    private RocksDBConfig rocksDBConfig;

    private RocksMap<byte[], AddressUtxo> utxoMap;
    private RocksMap<byte[], TxInput> spentUtxoMap;
    private RocksZSet<byte[]> utxoSlotZSet;
    private RocksZSet<byte[]> spentUtxoSlotZSet;

    private RocksMultiZSet<byte[]> addressUtxoZSet;
    private RocksMultiZSet<byte[]> paymentCredUtxoZSet;

    private final UtxoCache utxoCache;
    private final List<TxInput> spentUtxoCache = Collections.synchronizedList(new ArrayList<>());

    @Value("${store.extensions.rocksdb-utxo-storage.write-batch-size:1000}")
    private int batchSize = 1000;

    @Value("${store.extensions.rocksdb-utxo-storage.parallel-write:false}")
    private boolean parallelWrites = false;

    public RocksDBUtxoStorage(RocksDBConfig rocksDBConfig, UtxoCache utxoCache) {
        this.rocksDBConfig = rocksDBConfig;
        this.utxoCache = utxoCache;
        this.utxoMap = new RocksMap<>(rocksDBConfig, UTXOS_COL_FAMILY, byte[].class, AddressUtxo.class);
        this.spentUtxoMap = new RocksMap<>(rocksDBConfig, SPENT_UTXOS_COL_FAMILY, byte[].class, TxInput.class);
        this.utxoSlotZSet = new RocksZSet<>(rocksDBConfig, UTXOS_COL_FAMILY, "slot_utxos", byte[].class);
        this.spentUtxoSlotZSet = new RocksZSet<>(rocksDBConfig, SPENT_UTXOS_COL_FAMILY, "spent_slot_utxos", byte[].class);

        this.addressUtxoZSet = new RocksMultiZSet<>(rocksDBConfig, UTXOS_COL_FAMILY, "address_utxos", byte[].class);
        this.paymentCredUtxoZSet = new RocksMultiZSet<>(rocksDBConfig, UTXOS_COL_FAMILY, "payment_cred_utxos", byte[].class);

        log.info("<< RocksDB utxo storage enabled >>");
    }

    @Override
    public void saveUnspent(List<AddressUtxo> addressUtxoList) {
        if (addressUtxoList.size() == 0) return;

        addressUtxoList.stream().forEach(utxo -> utxoCache.add(utxo));
    }

    @SneakyThrows
    @Override
    public void saveSpent(List<TxInput> txInputs) {
        if (txInputs.size() == 0) return;

        spentUtxoCache.addAll(txInputs);
    }

    @Override
    public Optional<AddressUtxo> findById(String txHash, int outputIndex) {
        var cacheUtxo = utxoCache.get(txHash, outputIndex);
        if (cacheUtxo.isPresent()) return cacheUtxo;
        else {
            var savedUtxo = utxoMap.get(getKey(txHash, outputIndex));
            savedUtxo.ifPresent(utxoCache::add);

            return savedUtxo;
        }
    }

    @Override
    public List<AddressUtxo> findAllByIds(List<UtxoKey> utxoKeys) {
        if (utxoKeys.size() == 0) return Collections.emptyList();

        var cacheResult = utxoCache.get(utxoKeys);
        if (cacheResult._2 == null) return cacheResult._1;

        List<UtxoKey> notFoundKeys = cacheResult._2;
        var keysToSearch = notFoundKeys.stream().map(utxoKey -> getKey(utxoKey.getTxHash(), utxoKey.getOutputIndex())).toList();

        var savedUtxos = utxoMap.multiGet(keysToSearch);

        //Add remaining utxos to cache
        if (savedUtxos != null) savedUtxos.forEach(utxoCache::add);

        List<AddressUtxo> finalUtxos = new ArrayList<>();
        finalUtxos.addAll(cacheResult._1);
        finalUtxos.addAll(savedUtxos);

        return finalUtxos;
    }

    @SneakyThrows
    @Override
    public int deleteUnspentBySlotGreaterThan(Long slot) {
        Long slotToDel = slot + 1;
        try (ValueIterator<Tuple<byte[], Long>> iterator = utxoSlotZSet.membersInRangeIterable(slotToDel, Long.MAX_VALUE);
             var writeBatch = new WriteBatch();
             var writeOptions = new WriteOptions()) {
            int counter = 0;
            while (iterator.hasNext()) {
                Tuple<byte[], Long> utxoIdWithSlot = iterator.next();

                var addressUtxo = utxoMap.get(utxoIdWithSlot._1);

                utxoMap.removeBatch(writeBatch, utxoIdWithSlot._1);
                utxoSlotZSet.removeBatch(writeBatch, utxoIdWithSlot._1);

                byte[] addressNS = getAddressBytes(addressUtxo.get().getOwnerAddr());
                addressUtxoZSet.removeBatch(addressNS, writeBatch, utxoIdWithSlot._1);

                byte[] paymentCredNS = getPaymentCredential(addressUtxo.get().getOwnerPaymentCredential());
                if (paymentCredNS != null)
                    paymentCredUtxoZSet.removeBatch(paymentCredNS, writeBatch, utxoIdWithSlot._1);
                counter++;
            }

            rocksDBConfig.getRocksDB().write(writeOptions, writeBatch);
            return counter;
        }
    }

    @SneakyThrows
    @Override
    public int deleteSpentBySlotGreaterThan(Long slot) {
        Long slotToDel = slot + 1;
        try (ValueIterator<Tuple<byte[], Long>> iterator = spentUtxoSlotZSet.membersInRangeIterable(slotToDel, Long.MAX_VALUE);
             var writeBatch = new WriteBatch();
             var writeOptions = new WriteOptions()) {
            int counter = 0;
            while (iterator.hasNext()) {
                Tuple<byte[], Long> utxoIdWithSlot = iterator.next();
                spentUtxoMap.removeBatch(writeBatch, utxoIdWithSlot._1);
                spentUtxoSlotZSet.removeBatch(writeBatch, utxoIdWithSlot._1);

                //Add to address utxo zset & payment cred utxo zset -- rollback
                utxoMap.get(utxoIdWithSlot._1).ifPresent(addressUtxo -> {
                    byte[] addressNS = getAddressBytes(addressUtxo.getOwnerAddr());
                    addressUtxoZSet.addBatch(addressNS, writeBatch, new Tuple<>(utxoIdWithSlot._1, addressUtxo.getSlot()));

                    byte[] paymentCredNS = getPaymentCredential(addressUtxo.getOwnerPaymentCredential());
                    if (paymentCredNS != null)
                        paymentCredUtxoZSet.addBatch(paymentCredNS, writeBatch, new Tuple<>(utxoIdWithSlot._1, addressUtxo.getSlot()));
                });

                counter++;
            }

            rocksDBConfig.getRocksDB().write(writeOptions, writeBatch);
            return counter;
        }

    }

    @Override
    public int deleteBySpentAndBlockLessThan(Long block) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @SneakyThrows
    @EventListener
    public void handleCommitEvent(CommitEvent commitEvent) {
        try {
            Collection<AddressUtxo> addressUtxoList = utxoCache.getAll();

            var utxoList = addressUtxoList.stream().map(addressUtxo -> new Tuple<>(getKey(addressUtxo.getTxHash(), addressUtxo.getOutputIndex()), addressUtxo)).toList();

            if (utxoList.size() > 1000) log.info("Saving {} utxos ", utxoList.size() + " - Batch size: " + batchSize + " - Parallel writes: " + parallelWrites);

            if (parallelWrites) {
                ListUtil.partitionAndApplyInParallel(utxoList, batchSize, tuples -> {
                    saveUnspentUtxosToDB(tuples);
                });
            } else {
                ListUtil.partitionAndApply(utxoList, batchSize, tuples -> {
                    saveUnspentUtxosToDB(tuples);
                });
            }

            handleCommitForSpentUtxos();
        } finally {
            utxoCache.clear();
            spentUtxoCache.clear();
        }
    }

    private void handleCommitForSpentUtxos() {
        if (spentUtxoCache.size() == 0)
            return;

        if (spentUtxoCache.size() > 1000) log.info("Saving {} spent utxos ", spentUtxoCache.size());

        if (parallelWrites) {
            ListUtil.partitionAndApplyInParallel(spentUtxoCache, batchSize, txInputs -> {
                saveSpentUtxosToDB(txInputs);
            });
        } else {
            ListUtil.partitionAndApply(spentUtxoCache, batchSize, txInputs -> {
                saveSpentUtxosToDB(txInputs);
            });
        }
    }

    private void saveUnspentUtxosToDB(List<Tuple<byte[], AddressUtxo>> tuples) {
        try (var writeBatch = new WriteBatch();
             var writeOptions = new WriteOptions()) {
            tuples.stream().forEach(tuple -> {
                utxoMap.putBatch(writeBatch, tuple);
                utxoSlotZSet.addBatch(writeBatch, new Tuple<>(tuple._1, tuple._2.getSlot()));

                byte[] addressNS = getAddressBytes(tuple._2.getOwnerAddr());
                addressUtxoZSet.addBatch(addressNS, writeBatch, new Tuple<>(tuple._1, tuple._2.getSlot()));

                byte[] paymentCredNS = getPaymentCredential(tuple._2.getOwnerPaymentCredential());
                if (paymentCredNS != null)
                    paymentCredUtxoZSet.addBatch(paymentCredNS, writeBatch, new Tuple<>(tuple._1, tuple._2.getSlot()));
            });

            rocksDBConfig.getRocksDB().write(writeOptions, writeBatch);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void saveSpentUtxosToDB(List<TxInput> txInputs) {
        try (var writeBatch = new WriteBatch();
             var writeOptions = new WriteOptions()) {
            txInputs.stream().forEach(txInput -> {
                spentUtxoMap.putBatch(writeBatch, new Tuple<>(getKey(txInput.getTxHash(), txInput.getOutputIndex()), txInput));
                spentUtxoSlotZSet.addBatch(writeBatch, new Tuple<>(getKey(txInput.getTxHash(), txInput.getOutputIndex()), txInput.getSpentAtSlot()));

                //Update address utxo zset & payment cred utxo zset
                var addressUtxo = findById(txInput.getTxHash(), txInput.getOutputIndex());

                if (addressUtxo.isPresent()) {
                    byte[] addressNS = getAddressBytes(addressUtxo.get().getOwnerAddr());
                    addressUtxoZSet.removeBatch(addressNS, writeBatch, getKey(txInput.getTxHash(), txInput.getOutputIndex()));

                    byte[] paymentCredNS = getPaymentCredential(addressUtxo.get().getOwnerPaymentCredential());
                    if (paymentCredNS != null)
                        paymentCredUtxoZSet.removeBatch(paymentCredNS, writeBatch, getKey(txInput.getTxHash(), txInput.getOutputIndex()));

                } else {
                    throw new RuntimeException("AddressUtxo not found for spent utxo: " + txInput.getTxHash() + ":" + txInput.getOutputIndex());
                }
            });

            rocksDBConfig.getRocksDB().write(writeOptions, writeBatch);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}

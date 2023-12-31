package com.bloxbean.cardano.yaci.store.extensions.utxo.rocksdb;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.TxInput;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
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
import org.rocksdb.RocksDBException;
import org.rocksdb.WriteBatch;
import org.rocksdb.WriteOptions;
import org.springframework.context.event.EventListener;

import java.util.*;

import static com.bloxbean.cardano.yaci.store.extensions.utxo.rocksdb.UtxoStoreKeyUtils.*;

@Slf4j
public class RocksDBUtxoStorage implements UtxoStorage {
    private static final String UTXOS_COL_FAMILY = "utxos";
    private static final String SPENT_UTXOS_COL_FAMILY = "spent_utxos";

    private RocksDBConfig rocksDBConfig;

    private RocksMap<String, AddressUtxo> utxoMap;
    private RocksMap<String, TxInput> spentUtxoMap;
    private RocksZSet<String> utxoSlotZSet;
    private RocksZSet<String> spentUtxoSlotZSet;

    private RocksMultiZSet<String> addressUtxoZSet;
    private RocksMultiZSet<String> paymentCredUtxoZSet;

    private final UtxoCache utxoCache;
    private final List<TxInput> spentUtxoCache = Collections.synchronizedList(new ArrayList<>());

    public RocksDBUtxoStorage(RocksDBConfig rocksDBConfig, UtxoCache utxoCache) {
        this.rocksDBConfig = rocksDBConfig;
        this.utxoCache = utxoCache;
        this.utxoMap = new RocksMap<>(rocksDBConfig, UTXOS_COL_FAMILY, String.class, AddressUtxo.class);
        this.spentUtxoMap = new RocksMap<>(rocksDBConfig, SPENT_UTXOS_COL_FAMILY, String.class, TxInput.class);
        this.utxoSlotZSet = new RocksZSet<>(rocksDBConfig, UTXOS_COL_FAMILY, "slot_utxos", String.class);
        this.spentUtxoSlotZSet = new RocksZSet<>(rocksDBConfig, SPENT_UTXOS_COL_FAMILY, "spent_slot_utxos", String.class);

        this.addressUtxoZSet = new RocksMultiZSet<>(rocksDBConfig, UTXOS_COL_FAMILY, "address_utxos", String.class);
        this.paymentCredUtxoZSet = new RocksMultiZSet<>(rocksDBConfig, UTXOS_COL_FAMILY, "payment_cred_utxos", String.class);

        log.info("<< Embedded utxo storage enabled >>");
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
        try (ValueIterator<Tuple<String, Long>> iterator = utxoSlotZSet.membersInRangeIterable(slotToDel, Long.MAX_VALUE)) {
            WriteBatch writeBatch = new WriteBatch();
            int counter = 0;
            while (iterator.hasNext()) {
                Tuple<String, Long> utxoIdWithSlot = iterator.next();

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

            rocksDBConfig.getRocksDB().write(new WriteOptions(), writeBatch);
            return counter;
        }
    }

    @SneakyThrows
    @Override
    public int deleteSpentBySlotGreaterThan(Long slot) {
        Long slotToDel = slot + 1;
        try (ValueIterator<Tuple<String, Long>> iterator = spentUtxoSlotZSet.membersInRangeIterable(slotToDel, Long.MAX_VALUE)) {
            WriteBatch writeBatch = new WriteBatch();
            int counter = 0;
            while (iterator.hasNext()) {
                Tuple<String, Long> utxoIdWithSlot = iterator.next();
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

            rocksDBConfig.getRocksDB().write(new WriteOptions(), writeBatch);
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


            WriteBatch writeBatch = new WriteBatch();
            if (utxoList.size() > 1000) log.info("Saving {} utxos ", utxoList.size());

            utxoList.stream().forEach(tuple -> {
                utxoMap.putBatch(writeBatch, tuple);
                utxoSlotZSet.addBatch(writeBatch, new Tuple<>(tuple._1, tuple._2.getSlot()));

                byte[] addressNS = getAddressBytes(tuple._2.getOwnerAddr());
                addressUtxoZSet.addBatch(addressNS, writeBatch, new Tuple<>(tuple._1, tuple._2.getSlot()));

                byte[] paymentCredNS = getPaymentCredential(tuple._2.getOwnerPaymentCredential());
                if (paymentCredNS != null)
                    paymentCredUtxoZSet.addBatch(paymentCredNS, writeBatch, new Tuple<>(tuple._1, tuple._2.getSlot()));
            });

            rocksDBConfig.getRocksDB().write(new WriteOptions(), writeBatch);

            handleCommitForSpentUtxos();
        } finally {
            utxoCache.clear();
            spentUtxoCache.clear();
        }
    }

    private void handleCommitForSpentUtxos() throws RocksDBException {
        if (spentUtxoCache.size() == 0)
            return;

        var utxoList = spentUtxoCache.stream().map(txInput -> new Tuple<>(getKey(txInput.getTxHash(), txInput.getOutputIndex()), txInput)).toList();

        var spentUtxoToSlotList = spentUtxoCache.stream().map(txInput -> new Tuple<>(getKey(txInput.getTxHash(), txInput.getOutputIndex()), txInput.getSpentAtSlot())).toList();

        WriteBatch writeBatch = new WriteBatch();
        spentUtxoMap.putBatch(writeBatch, utxoList.toArray(new Tuple[0]));
        spentUtxoSlotZSet.addBatch(writeBatch, spentUtxoToSlotList.toArray(new Tuple[0]));

        //Update address utxo zset & payment cred utxo zset
        spentUtxoCache.stream().forEach(txInput -> {
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

        rocksDBConfig.getRocksDB().write(new WriteOptions(), writeBatch);

    }

}

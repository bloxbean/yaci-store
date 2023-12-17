package com.bloxbean.cardano.yaci.store.extensions.utxo.rocksdb;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.TxInput;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import com.bloxbean.cardano.yaci.store.common.util.ListUtil;
import com.bloxbean.cardano.yaci.store.events.internal.CommitEvent;
import com.bloxbean.cardano.yaci.store.rocksdb.*;
import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorage;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.UtxoCache;
import com.bloxbean.rocks.types.common.IndexRecord;
import com.bloxbean.rocks.types.common.KeyValue;
import com.bloxbean.rocks.types.config.RocksDBConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.bloxbean.cardano.yaci.store.extensions.utxo.rocksdb.UtxoStoreKeyUtils.*;

@Slf4j
public class EmbeddedUtxoStorage implements UtxoStorage {
    public static final String UTXOS_COL_FAMILY = "utxos";
    public static final String SPENT_UTXOS_COL_FAMILY = "spent-utxos";
    public static final String UTXOS_SLOT_COL_FAMILY = "utxos_slot";
    public static final String SPENT_UTXOS_SLOT_COL_FAMILY = "spent-utxos_slot";
    public static final String UTXOS_BY_PAYMENT_CRED_COL_FAMILY = "utxos-by-payment-cred";
    public static final String UTXOS_BY_PAYMENT_CRED_ASSET_COL_FAMILY = "utxos-by-payment-cred-asset";

    private RocksDBRepository utxoRepository;
    private RocksDBRepository spentUtxoRepository;

    @Value("${store.aggr.embedded-utxo-storage.write-batch-size:1000}")
    private int batchSize;

    private final UtxoCache utxoCache;

    public EmbeddedUtxoStorage(RocksDBConfig rocksDBConfig, UtxoCache utxoCache) {
        this.utxoCache = utxoCache;
        this.utxoRepository = new RocksDBRepository<AddressUtxo>(rocksDBConfig, UTXOS_COL_FAMILY, true)
                .withIndex(UTXOS_BY_PAYMENT_CRED_COL_FAMILY,
                        (utxo) -> List.of(getKeyForPaymentCred(utxo)))
                .withIndex(UTXOS_BY_PAYMENT_CRED_ASSET_COL_FAMILY, (utxo) -> getKeyForPaymentCredAndAsset(utxo))
                .withIndex(UTXOS_SLOT_COL_FAMILY, (utxo) -> getSlotKey(utxo));

        this.spentUtxoRepository = new RocksDBRepository<TxInput>(rocksDBConfig, SPENT_UTXOS_COL_FAMILY, true)
                .withIndex(SPENT_UTXOS_SLOT_COL_FAMILY, (input) ->  getTxInputSlotKey(input));
        log.info("<< Embedded utxo storage enabled >>");
    }

    @Override
    public void saveUnspent(List<AddressUtxo> addressUtxoList) {
        if (addressUtxoList.size() == 0)
            return;

        addressUtxoList.stream()
                .forEach(utxo -> utxoCache.add(utxo));
    }

    @Override
    public void saveSpent(List<TxInput> addressUtxoList) {
        if (addressUtxoList.size() == 0)
            return;

        var utxoList = addressUtxoList.stream()
                .map(txInput -> new KeyValue(getKey(txInput.getTxHash(), txInput.getOutputIndex()), txInput))
                .toList();

        spentUtxoRepository.saveBatch(utxoList);

        long slot = addressUtxoList.get(0).getSpentAtSlot();
        var keys = utxoList.stream()
                .map(keyValue -> (String) keyValue.getKey())
                .toList();

        spentUtxoRepository.createSlotIndex(slot, keys);
    }

    @Override
    public Optional<AddressUtxo> findById(String txHash, int outputIndex) {
        return utxoRepository.find(getKey(txHash, outputIndex), AddressUtxo.class);
    }

    @Override
    public List<AddressUtxo> findAllByIds(List<UtxoKey> utxoKeys) {
        if (utxoKeys.size() == 0)
            return Collections.emptyList();

        var keys = utxoKeys.stream()
                .map(utxoKey -> getKey(utxoKey.getTxHash(), utxoKey.getOutputIndex()))
                .toList();

        return utxoRepository.findMulti(keys, AddressUtxo.class);
    }

    @Override
    public int deleteUnspentBySlotGreaterThan(Long slot) {
        Long slotToDel = slot + 1;
        List<KeyValue> slotUtxoKeys = utxoRepository.findAllByIndex(UTXOS_SLOT_COL_FAMILY, String.valueOf(slotToDel), List.class);

        List<UtxoKey> utxoKeys = slotUtxoKeys.stream()
                .map(keyValue -> fromSlotKey((String) keyValue.getKey()))
                .toList();

        List<IndexRecord> indexRecordsToDelete = slotUtxoKeys.stream()
                .map(keyValue -> new IndexRecord((String)keyValue.getKey(), (String)keyValue.getValue()))
                .toList();

        if (utxoKeys.size() > 0) {
            utxoRepository.deleteMulti(utxoKeys);

            utxoRepository.deleteIndex(UTXOS_SLOT_COL_FAMILY, indexRecordsToDelete);
            return utxoKeys.size();
        } else {
            return 0;
        }
    }

    @Override
    public int deleteSpentBySlotGreaterThan(Long slot) {

        Long slotToDel = slot + 1;
        List<KeyValue> slotUtxoKeys = spentUtxoRepository.findAllByIndex(SPENT_UTXOS_SLOT_COL_FAMILY, String.valueOf(slotToDel), List.class);

        List<UtxoKey> utxoKeys = slotUtxoKeys.stream()
                .map(keyValue -> fromSlotKey((String) keyValue.getKey()))
                .toList();

        List<IndexRecord> indexRecordsToDelete = slotUtxoKeys.stream()
                .map(keyValue -> new IndexRecord((String)keyValue.getKey(), (String)keyValue.getValue()))
                .toList();

        if (utxoKeys.size() > 0) {
            spentUtxoRepository.deleteMulti(utxoKeys);
            spentUtxoRepository.deleteIndex(SPENT_UTXOS_SLOT_COL_FAMILY, indexRecordsToDelete);
            return utxoKeys.size();
        } else {
            return 0;
        }
    }

    @Override
    public int deleteBySpentAndBlockLessThan(Long block) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @EventListener
    public void handleCommitEvent(CommitEvent commitEvent) {
        try {
            Collection<AddressUtxo> addressUtxoList = utxoCache.getAll();
            addressUtxoList.stream()
                    .forEach(utxo -> utxoCache.add(utxo));

            var utxoList = addressUtxoList.stream()
                    .map(addressUtxo -> new KeyValue<String, AddressUtxo>(getKey(addressUtxo.getTxHash(), addressUtxo.getOutputIndex()), addressUtxo))
                    .toList();

            if (utxoList.size() > 1000)
                log.info("Saving {} utxos ", utxoList.size());
            ListUtil.partition(utxoList, batchSize)
                    .forEach(utxos -> utxoRepository.saveBatch(utxos));

            var keyList = utxoList.stream()
                    .map(KeyValue::getKey)
                    .toList();

            utxoRepository.createSlotIndex(commitEvent.getMetadata().getSlot(), keyList);

        } finally {
            utxoCache.clear();
        }
    }
}

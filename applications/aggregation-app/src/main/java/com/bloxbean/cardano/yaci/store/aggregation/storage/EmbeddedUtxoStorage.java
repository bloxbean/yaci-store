package com.bloxbean.cardano.yaci.store.aggregation.storage;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.TxInput;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import com.bloxbean.cardano.yaci.store.rocksdb.RocksDBConfig;
import com.bloxbean.cardano.yaci.store.rocksdb.RocksDBRepository;
import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorage;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Slf4j
public class EmbeddedUtxoStorage implements UtxoStorage {
    public static final String UTXOS_COL_FAMILY = "utxos";
    public static final String SPENT_UTXOS_COL_FAMILY = "spent-utxos";

    private RocksDBRepository utxoRepository;
    private RocksDBRepository spentUtxoRepository;

    public EmbeddedUtxoStorage(RocksDBConfig rocksDBConfig) {
        this.utxoRepository = new RocksDBRepository(rocksDBConfig, UTXOS_COL_FAMILY);
        this.spentUtxoRepository = new RocksDBRepository(rocksDBConfig, SPENT_UTXOS_COL_FAMILY);
        log.info("<< Embedded utxo storage enabled >>");
    }

    @Override
    public void saveUnspent(List<AddressUtxo> addressUtxoList) {
        if (addressUtxoList.size() == 0)
            return;

        addressUtxoList.stream()
                .forEach(addressUtxo -> utxoRepository.save(getKey(addressUtxo.getTxHash(), addressUtxo.getOutputIndex()), addressUtxo));
    }

    @Override
    public void saveSpent(List<TxInput> addressUtxoList) {
        if (addressUtxoList.size() == 0)
            return;

        addressUtxoList.stream()
                .forEach(txInput -> {
                    spentUtxoRepository.save(getKey(txInput.getTxHash(), txInput.getOutputIndex()), txInput);
                });
    }

    @Override
    public Optional<AddressUtxo> findById(String txHash, int outputIndex) {
        return utxoRepository.find(getKey(txHash, outputIndex), AddressUtxo.class);
    }

    @Override
    public List<AddressUtxo> findAllByIds(List<UtxoKey> utxoKeys) {
        return utxoKeys.stream()
                .map(utxoKey -> utxoRepository.find(getKey(utxoKey.getTxHash(), utxoKey.getOutputIndex()), AddressUtxo.class))
                .filter(optional -> optional.isPresent())
                .map(optional -> optional.get())
                .map(o -> (AddressUtxo) o)
                .toList();
    }

    @Override
    public int deleteUnspentBySlotGreaterThan(Long slot) {
        return 0; //TODO
    }

    @Override
    public int deleteSpentBySlotGreaterThan(Long slot) {
        return 0; //TODO
    }

    private String getKey(String txHash, int outputIndex) {
        return txHash + "#" + outputIndex;
    }
}

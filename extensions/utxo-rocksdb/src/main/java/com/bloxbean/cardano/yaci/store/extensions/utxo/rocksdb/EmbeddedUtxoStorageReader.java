package com.bloxbean.cardano.yaci.store.extensions.utxo.rocksdb;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.yaci.core.util.HexUtil;
import com.bloxbean.cardano.yaci.core.util.Tuple;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.TxInput;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.rocksdb.RocksDBRepository;
import com.bloxbean.cardano.yaci.store.rocksdb.common.KeyValue;
import com.bloxbean.cardano.yaci.store.rocksdb.config.RocksDBConfig;
import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorageReader;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

import static com.bloxbean.cardano.yaci.store.extensions.utxo.rocksdb.EmbeddedUtxoStorage.*;
import static com.bloxbean.cardano.yaci.store.extensions.utxo.rocksdb.UtxoStoreKeyUtils.*;

@Slf4j
public class EmbeddedUtxoStorageReader implements UtxoStorageReader {
    private RocksDBRepository<AddressUtxo> utxoRepository;
    private RocksDBRepository spentUtxoRepository;

    public EmbeddedUtxoStorageReader(RocksDBConfig rocksDBConfig) {
        this.utxoRepository = new RocksDBRepository<AddressUtxo>(rocksDBConfig, UTXOS_COL_FAMILY)
                .withIndex(UTXOS_BY_PAYMENT_CRED_COL_FAMILY,
                        (utxo) -> List.of(getKeyForPaymentCred(utxo)))
                .withIndex(UTXOS_BY_PAYMENT_CRED_ASSET_COL_FAMILY, (utxo) -> getKeyForPaymentCredAndAsset(utxo))
                .withIndex(UTXOS_SLOT_COL_FAMILY, (utxo) -> getSlotKey(utxo));

        this.spentUtxoRepository = new RocksDBRepository<TxInput>(rocksDBConfig, SPENT_UTXOS_COL_FAMILY)
                .withIndex(SPENT_UTXOS_SLOT_COL_FAMILY, (input) ->  getTxInputSlotKey(input));
        log.info("<< Embedded utxo storage reader enabled >>");
    }

    @Override
    public Optional<AddressUtxo> findById(String txHash, int outputIndex) {
        return utxoRepository.find(getKey(txHash, outputIndex), AddressUtxo.class);
    }

    @Override
    public List<AddressUtxo> findAllByIds(List<UtxoKey> utxoKeys) {
        var keys = utxoKeys.stream()
                .map(utxoKey -> getKey(utxoKey.getTxHash(), utxoKey.getOutputIndex()))
                .toList();

        return utxoRepository.findMulti(keys, AddressUtxo.class);
    }

    @Override
    public List<AddressUtxo> findUtxoByAddress(String address, int page, int count, Order order) {
        Address addr = new Address(address);
        String paymentCred = addr.getPaymentCredential()
                .map(cred -> HexUtil.encodeHexString(cred.getBytes()))
                .orElseThrow(() -> new IllegalArgumentException("Invalid address"));

        return findUtxoByPaymentCredential(paymentCred, page, count, order);
    }

    @Override
    public List<AddressUtxo> findUtxoByAddressAndAsset(String ownerAddress, String unit, int page, int count, Order order) {
        Address addr = new Address(ownerAddress);
        String paymentCred = addr.getPaymentCredential()
                .map(cred -> HexUtil.encodeHexString(cred.getBytes()))
                .orElseThrow(() -> new IllegalArgumentException("Invalid address"));

       return findUtxoByPaymentCredentialAndAsset(paymentCred, unit, page, count, order);
    }

    @Override
    public List<AddressUtxo> findUtxoByPaymentCredential(String paymentCredential, int page, int count, Order order) {
        List<KeyValue> keyValues = utxoRepository.findAllByIndex(UTXOS_BY_PAYMENT_CRED_COL_FAMILY, paymentCredential, Boolean.class);

        var utxoKeys = keyValues.stream()
              //TODO  .filter(keyValue -> keyValue.getValue() != null)
                .map(keyValue ->  fromPaymentCredKey((String) keyValue.getKey())).toList();

        return findAllByIds(utxoKeys);
    }

    @Override
    public List<AddressUtxo> findUtxoByPaymentCredentialAndAsset(String paymentCredential, String unit, int page, int count, Order order) {
        String paymentCredUnit = createHash(paymentCredential, unit);
        List<KeyValue> keyValues = utxoRepository.findAllByIndex(UTXOS_BY_PAYMENT_CRED_ASSET_COL_FAMILY,
                paymentCredUnit, Boolean.class);

        var utxoKeys = keyValues.stream()
               //TODO .filter(keyValue -> keyValue.getValue() != null)
                .map(keyValue ->  fromPaymentCredAndAssetKey((String) keyValue.getKey())).toList();

        if (log.isDebugEnabled())
            log.debug(utxoKeys.toString());
        return findAllByIds(utxoKeys);
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

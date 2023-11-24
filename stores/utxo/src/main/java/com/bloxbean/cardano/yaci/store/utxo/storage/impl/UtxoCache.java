package com.bloxbean.cardano.yaci.store.utxo.storage.impl;

import com.bloxbean.cardano.yaci.core.util.Tuple;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Slf4j
public class UtxoCache { //TODO -- auto expiry
    private Map<String, AddressUtxo> utxoCache = Collections.synchronizedMap(new HashMap<>());

    @SneakyThrows
    public Optional<AddressUtxo> get(String txHash, int outputIndex) {
        String key = getUtxoKey(txHash, outputIndex);
        var addrUtxo = utxoCache.get(key);
        if (addrUtxo == null)
            return Optional.empty();
        else
            return Optional.ofNullable(addrUtxo);
    }

    @SneakyThrows
    public void add(@NonNull AddressUtxo addressUtxo) {
        String key = getUtxoKey(addressUtxo.getTxHash(), addressUtxo.getOutputIndex());
        utxoCache.put(key, addressUtxo);
    }

    @SneakyThrows
    public Tuple<List<AddressUtxo>, List<UtxoKey>> get(List<UtxoKey> utxoKeyList) {
        List<UtxoKey> notFoundList = null;
        List<AddressUtxo> foundList = new ArrayList<>();
        for (var utxoKey: utxoKeyList) {
            var addressUtxoOptional = get(utxoKey.getTxHash(), utxoKey.getOutputIndex());
            if (addressUtxoOptional.isPresent())
                foundList.add(addressUtxoOptional.get());
            else {
                if (notFoundList  == null)
                    notFoundList = new ArrayList<>();

                notFoundList.add(utxoKey);
            }
        }

        return new Tuple<>(foundList, notFoundList);
    }

    public void clear() {
        log.info("Total utxos: " + utxoCache.size());
        utxoCache.clear();
    }

    private String getUtxoKey(String txHash, int outputIndex) {
        return txHash + ":" + outputIndex;
    }
}

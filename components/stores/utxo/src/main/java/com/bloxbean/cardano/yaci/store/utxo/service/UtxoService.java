package com.bloxbean.cardano.yaci.store.utxo.service;

import com.bloxbean.carano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.carano.yaci.store.common.domain.UtxoKey;
import com.bloxbean.cardano.yaci.store.utxo.storage.api.UtxoStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class UtxoService {
    private final UtxoStorage utxoStorage;

    public Optional<AddressUtxo> getUtxo(String txHash, int index) {
        return utxoStorage.findById(txHash, index);
    }

    public List<AddressUtxo> getUtxos(List<UtxoKey> utxoIds) {
        return utxoStorage.findAllByIds(utxoIds);
    }
}

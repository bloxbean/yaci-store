package com.bloxbean.cardano.yaci.store.client.utxo;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Utxo;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
public class DummyUtxoClient implements UtxoClient {
    public DummyUtxoClient() {
        log.warn("Dummy Utxo Client Configured >>>>>>>>");
    }

    @Override
    public List<AddressUtxo> getUtxosByIds(List<UtxoKey> utxoIds) {
        return Collections.emptyList();
    }

    @Override
    public Optional<AddressUtxo> getUtxoById(UtxoKey utxoId) {
        return Optional.empty();
    }

    @Override
    public List<Utxo> getUtxoByAddress(String address, int page, int count) {
        return Collections.emptyList();
    }
}

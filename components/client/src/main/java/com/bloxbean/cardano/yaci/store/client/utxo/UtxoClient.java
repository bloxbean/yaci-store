package com.bloxbean.cardano.yaci.store.client.utxo;

import com.bloxbean.carano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.carano.yaci.store.common.domain.UtxoKey;

import java.util.List;
import java.util.Optional;

public interface UtxoClient {
    List<AddressUtxo> getUtxosByIds(List<UtxoKey> utxoIds);
    Optional<AddressUtxo> getUtxoById(UtxoKey utxoId);
}

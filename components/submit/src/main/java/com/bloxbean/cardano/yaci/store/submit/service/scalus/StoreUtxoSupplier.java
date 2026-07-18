package com.bloxbean.cardano.yaci.store.submit.service.scalus;

import com.bloxbean.cardano.client.api.UtxoSupplier;
import com.bloxbean.cardano.client.api.common.OrderEnum;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.yaci.store.client.utxo.UtxoClient;
import com.bloxbean.cardano.yaci.store.common.ccl.CclUtxoMapper;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;

import java.util.List;
import java.util.Optional;

class StoreUtxoSupplier implements UtxoSupplier {
    private final UtxoClient utxoClient;
    private final ReferenceScriptSupplier scriptSupplier;

    StoreUtxoSupplier(UtxoClient utxoClient, ReferenceScriptSupplier scriptSupplier) {
        this.utxoClient = utxoClient;
        this.scriptSupplier = scriptSupplier;
    }

    @Override
    public List<Utxo> getPage(String address, Integer page, Integer count, OrderEnum order) {
        throw new UnsupportedOperationException("Address-page UTxO lookup is not used for transaction evaluation; getTxOutput resolves inputs by transaction id.");
    }

    @Override
    public Optional<Utxo> getTxOutput(String txHash, int outputIndex) {
        return utxoClient.getUtxoById(new UtxoKey(txHash, outputIndex))
                .map(addressUtxo -> {
                    // CCL Utxo stores only the reference script hash; Scalus resolves
                    // the script body later through ScriptSupplier during evaluation.
                    scriptSupplier.register(addressUtxo);
                    return CclUtxoMapper.fromAddressUtxo(addressUtxo);
                });
    }
}

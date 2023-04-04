package com.bloxbean.cardano.yaci.store.utxo.client;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import com.bloxbean.cardano.yaci.store.client.utxo.UtxoClient;
import com.bloxbean.cardano.yaci.store.utxo.storage.api.UtxoStorage;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component("utxoClient")
@Primary
@ConditionalOnProperty(name = "store.enable.local.utxo.client",
        havingValue = "true",
        matchIfMissing = true)
@Slf4j
public class UtxoClientImpl implements UtxoClient {
    private UtxoStorage utxoStorage;

    public UtxoClientImpl(UtxoStorage utxoStorage) {
        this.utxoStorage = utxoStorage;
        log.info("Enabled Local UtxoClient >>>");
    }

    @Override
    public List<AddressUtxo> getUtxosByIds(List<UtxoKey> utxoIds) {
        return utxoStorage.findAllByIds(utxoIds);
    }

    @Override
    public Optional<AddressUtxo> getUtxoById(@NotNull UtxoKey utxoId) {
        return utxoStorage.findById(utxoId.getTxHash(), utxoId.getOutputIndex());
    }
}

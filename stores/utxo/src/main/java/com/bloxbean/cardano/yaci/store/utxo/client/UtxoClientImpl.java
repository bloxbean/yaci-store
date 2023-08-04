package com.bloxbean.cardano.yaci.store.utxo.client;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Utxo;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import com.bloxbean.cardano.yaci.store.client.utxo.UtxoClient;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.utxo.service.AddressService;
import com.bloxbean.cardano.yaci.store.utxo.storage.api.UtxoStorage;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static com.bloxbean.cardano.yaci.store.common.util.Bech32Prefixes.STAKE_ADDR_PREFIX;
import static com.bloxbean.cardano.yaci.store.common.util.Bech32Prefixes.STAKE_ADDR_VKEY_HASH_PREFIX;

@Component("utxoClient")
@Primary
@ConditionalOnProperty(name = "store.enable.local.utxo.client",
        havingValue = "true",
        matchIfMissing = true)
@Slf4j
public class UtxoClientImpl implements UtxoClient {
    private UtxoStorage utxoStorage;
    private AddressService addressService;

    public UtxoClientImpl(UtxoStorage utxoStorage, AddressService addressService) {
        this.utxoStorage = utxoStorage;
        this.addressService = addressService;
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

    @Override
    public List<Utxo> getUtxoByAddress(@NotNull String address, int page, int count) {
        if (address == null)
            throw new IllegalArgumentException("Address cannot be null");

        if(address.startsWith(STAKE_ADDR_PREFIX) || address.startsWith(STAKE_ADDR_VKEY_HASH_PREFIX)) {
            return addressService.getUtxoByStakeAddress(address, page, count, Order.asc);
        } else {
            return addressService.getUtxoByAddress(address, page, count, Order.asc);
        }
    }
}

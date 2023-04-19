package com.bloxbean.cardano.yaci.store.utxo.service;

import com.bloxbean.cardano.yaci.store.common.domain.Utxo;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.utxo.storage.api.UtxoStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.yaci.store.utxo.service.UtxoUtil.addressUtxoToUtxo;

@Component
@RequiredArgsConstructor
@Slf4j
public class AddressService {
    private final UtxoStorage utxoStorage;

    public List<Utxo> getUtxoByAddress(String address, int page, int count, Order order) {
        return utxoStorage.findUtxoByAddress(address, page, count, order)
                .orElseGet(() -> Collections.emptyList())
                .stream()
                .map(addressUtxo -> addressUtxoToUtxo(addressUtxo)).collect(Collectors.toList());
    }

    public List<Utxo> getUtxoByAddressAndAsset(String address, String asset, int page, int count, Order order) {
        return utxoStorage.findUtxoByAddressAndAsset(address, asset, page, count, order)
                .orElseGet(() -> Collections.emptyList())
                .stream()
                .map(addressUtxo -> addressUtxoToUtxo(addressUtxo)).collect(Collectors.toList());
    }
}

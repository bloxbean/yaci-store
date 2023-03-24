package com.bloxbean.cardano.yaci.store.utxo.service;

import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.yaci.store.utxo.storage.api.UtxoStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.yaci.store.utxo.service.UtxoUtil.addressUtxoToUtxo;

@Component
@RequiredArgsConstructor
@Slf4j
public class AddressService {
    private final UtxoStorage utxoStorage;

    public List<Utxo> getUnspentUtxos(String address, int page, int count, String order) {
        Pageable pageable = PageRequest.of(page, count);
        return utxoStorage.findAddressUtxoByOwnerAddrAndSpent(address, null, pageable)
                .orElseGet(() -> new ArrayList<>())
                .stream()
                .map(addressUtxo -> addressUtxoToUtxo(addressUtxo)).collect(Collectors.toList());
    }
}

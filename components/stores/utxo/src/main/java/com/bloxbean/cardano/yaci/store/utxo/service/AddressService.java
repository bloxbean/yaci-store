package com.bloxbean.cardano.yaci.store.utxo.service;

import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.yaci.store.utxo.repository.UtxoRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AddressService {
    private UtxoRepository utxoRepository;

    public AddressService(UtxoRepository utxoRepository) {
        this.utxoRepository = utxoRepository;
    }

    public List<Utxo> getUnspentUtxos(String address, int page, int count, String order) {
        Pageable pageable = PageRequest.of(page, count);
        return utxoRepository.findAddressUtxoByOwnerAddrAndSpent(address, null, pageable)
                .orElseGet(() -> new ArrayList<>())
                .stream()
                .map(addressUtxo -> Utxo.builder()
                        .txHash(addressUtxo.getTxHash())
                        .outputIndex(addressUtxo.getOutputIndex())
                        .address(addressUtxo.getOwnerAddr())
                        .amount(addressUtxo.getAmounts().stream()
                                .map(amt -> new Amount(amt.getUnit(), amt.getQuantity()))
                                .collect(Collectors.toList()))
                        .dataHash(addressUtxo.getDataHash())
                        .inlineDatum(addressUtxo.getInlineDatum())
                        .referenceScriptHash(addressUtxo.getScriptRef())
                        .build()).collect(Collectors.toList());
    }
}

package com.bloxbean.cardano.yaci.store.utxo.service;

import com.bloxbean.carano.yaci.store.common.util.StringUtil;
import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.transaction.spec.PlutusData;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.store.utxo.repository.UtxoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
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
                .map(addressUtxo -> {
                        //If datahash is not set but inline datum is there, set datahash
                        String dataHash = addressUtxo.getDataHash();
                        try {
                            if (StringUtil.isEmpty(dataHash) && !StringUtil.isEmpty(addressUtxo.getInlineDatum())) {
                                byte[] inlineDatumBytes = HexUtil.decodeHexString(addressUtxo.getInlineDatum());
                                dataHash = PlutusData.deserialize(inlineDatumBytes).getDatumHash();
                            }
                        } catch (Exception e) {
                            log.error("Invalid inline datum found in utxo tx : {}, index: {}, inline_datum: {}", addressUtxo.getTxHash(), addressUtxo.getOutputIndex(), addressUtxo.getInlineDatum());
                        }

                        Utxo utxo = Utxo.builder()
                        .txHash(addressUtxo.getTxHash())
                        .outputIndex(addressUtxo.getOutputIndex())
                        .address(addressUtxo.getOwnerAddr())
                        .amount(addressUtxo.getAmounts().stream()
                                .map(amt -> {
                                    String unit = amt.getUnit();
                                    if (unit != null && unit.contains("."))
                                        unit = unit.replace(".", "");//TODO -- Done to make it compatible with Blockfrost or CCL backend
                                    return new Amount(unit, amt.getQuantity());
                                })
                                .collect(Collectors.toList()))
                        .dataHash(dataHash)
                        .inlineDatum(addressUtxo.getInlineDatum())
                        .referenceScriptHash(addressUtxo.getScriptRef())
                        .build();
                        return utxo;
                }).collect(Collectors.toList());
    }
}

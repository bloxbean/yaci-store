package com.bloxbean.cardano.yaci.store.utxo.service;

import com.bloxbean.cardano.client.transaction.spec.PlutusData;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Utxo;
import com.bloxbean.cardano.yaci.store.common.util.StringUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.stream.Collectors;

@Slf4j
class UtxoUtil {

    public static Utxo addressUtxoToUtxo(AddressUtxo addressUtxo) {
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
                            return amt.toBuilder().unit(unit).build();
                        })
                        .collect(Collectors.toList()))
                .dataHash(dataHash)
                .inlineDatum(addressUtxo.getInlineDatum())
                .referenceScriptHash(addressUtxo.getScriptRef())
                .build();
        return utxo;
    }
}

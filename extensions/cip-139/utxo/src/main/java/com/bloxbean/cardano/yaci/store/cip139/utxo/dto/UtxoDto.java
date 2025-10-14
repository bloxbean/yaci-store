package com.bloxbean.cardano.yaci.store.cip139.utxo.dto;

import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.core.model.NativeScript;
import com.bloxbean.cardano.yaci.core.model.PlutusScript;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.util.JsonUtil;
import com.bloxbean.cardano.yaci.store.common.util.ScriptReferenceUtil;
import com.bloxbean.cardano.yaci.store.script.domain.ScriptType;
import com.bloxbean.cardano.yaci.store.script.helper.ScriptUtil;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.bloxbean.cardano.yaci.core.util.Constants.LOVELACE;
import static com.bloxbean.cardano.yaci.store.script.helper.ScriptUtil.toPlutusScript;
import static com.bloxbean.cardano.yaci.store.script.helper.ScriptUtil.toScriptType;

@Slf4j
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class UtxoDto {

    private List<TransactionUnspentOutput> utxos;

    record TransactionUnspentOutput(TxInput input, TxOutput output){}

    record TxInput(String transactionId, Integer index){}

    record TxOutput(String address, TxAmount amount, PlutusData plutusData, ScriptRef scriptRef){}

    record TxAmount(BigInteger coin, Map<String, Map<String, BigInteger>> assets){}

    record PlutusData(String tag, String value){}

    record ScriptRef(String tag, @JsonProperty("value") PlutusScript plutusScript, @JsonProperty("value") NativeScript nativeScript){}

    record PlutusScript(String language, String bytes){}

    record NativeScript(String tag, String value){}


    public static UtxoDto fromDomain(List<AddressUtxo> addressUtxos){

        List<TransactionUnspentOutput> transactionUnspentOutputs = new ArrayList<>();

        addressUtxos.forEach(addressUtxo -> {

            TxInput txInput = new TxInput(addressUtxo.getTxHash(), addressUtxo.getOutputIndex());

            Map<String, Map<String, BigInteger>> multiassets = new LinkedHashMap<>();
            BigInteger coin = BigInteger.ZERO;
            for (var amt : addressUtxo.getAmounts()) {
                if ( amt.getUnit().equals(LOVELACE)){
                    coin = amt.getQuantity();
                }
                String policyId = amt.getPolicyId();
                Map<String, BigInteger> policyAssetMap = multiassets.getOrDefault(policyId, new LinkedHashMap<>());
                policyAssetMap.put(amt.getAssetName(), amt.getQuantity());
                multiassets.put(policyId, policyAssetMap);
            };
            TxAmount transactionAmount = new TxAmount(coin, multiassets);

            PlutusData plutusData = null;
            if (addressUtxo.getInlineDatum() != null && !addressUtxo.getInlineDatum().equals("")) {
                plutusData = new PlutusData("datum", addressUtxo.getInlineDatum());
            } else {
                plutusData = new PlutusData("datum_hash", addressUtxo.getDataHash());
            }

            ScriptRef scriptRef = null;
            com.bloxbean.cardano.client.spec.Script script;
            try {
                script = ScriptReferenceUtil.deserializeScriptRef(HexUtil.decodeHexString(addressUtxo.getScriptRef()));

                com.bloxbean.cardano.yaci.store.script.domain.ScriptType scriptType = toScriptType(script);
                String content;
                String scriptHash;

                if (scriptType == ScriptType.NATIVE_SCRIPT) {
                    com.bloxbean.cardano.yaci.core.model.NativeScript tempNativeScript = com.bloxbean.cardano.yaci.core.model.NativeScript.builder()
                            .type(script.getScriptType())
                            .content(JsonUtil.getJson(script))
                            .build();

                    content = JsonUtil.getJson(tempNativeScript);
                    try {
                        String nativeScriptTag = getNativeScriptTag(tempNativeScript.getType());
                        NativeScript nativeScript = new NativeScript(nativeScriptTag, content);
                        scriptRef = new ScriptRef("native_script", null, nativeScript);
                    } catch (Exception e) {
                        log.error("Error getting native script hash for script ref " + addressUtxo.getScriptRef() , e);
                        scriptRef = new ScriptRef("native_script", null, null);
                    }
                } else {
                    com.bloxbean.cardano.yaci.core.model.PlutusScript tempPlutusScript = toPlutusScript(script);

                    content = JsonUtil.getJson(tempPlutusScript);

                    try {
                        PlutusScript plutusScript = new PlutusScript(tempPlutusScript.getType().name(), content);
                        scriptRef = new ScriptRef("plutus_script", plutusScript, null);
                    } catch (Exception e) {
                        log.error("Error getting plutus script hash for script ref " + addressUtxo.getScriptRef(), e);
                        scriptRef = new ScriptRef("plutus_script", null, null);
                    }
                }

            } catch (Exception e) {
                log.error("Script deserialization failed for script ref " + addressUtxo.getScriptRef(), e);
            }

            TxOutput txOutput = new TxOutput(addressUtxo.getOwnerAddr(), transactionAmount, plutusData, scriptRef);

            transactionUnspentOutputs.add(new TransactionUnspentOutput(txInput, txOutput));
        });

        return new UtxoDto(transactionUnspentOutputs);
    }

    private static String getNativeScriptTag(int nativeScriptType) throws Exception {
        return switch (nativeScriptType) {
            case 0 -> "pubkey";
            case 1 -> "all";
            case 2 -> "any";
            case 3 -> "n_of_k";
            case 4 -> "timelock_start";
            case 5 -> "timelock_expiry";
            default -> throw new Exception ("Incorrect native script type");
        };
    }
}

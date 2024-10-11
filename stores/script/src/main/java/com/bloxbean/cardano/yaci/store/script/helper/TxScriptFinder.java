package com.bloxbean.cardano.yaci.store.script.helper;

import com.bloxbean.cardano.yaci.core.model.PlutusScript;
import com.bloxbean.cardano.yaci.core.util.HexUtil;
import com.bloxbean.cardano.yaci.helper.model.Transaction;
import com.bloxbean.cardano.yaci.store.client.utxo.UtxoClient;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import com.bloxbean.cardano.yaci.store.common.util.ScriptReferenceUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@AllArgsConstructor
@Slf4j
public class TxScriptFinder {
    private UtxoClient utxoClient;

    /**
     * Find all involved scripts from a transaction
     * <br>
     * Get PlutusV1, PlutusV2 scripts from witnessSet if available
     * <br>
     * Resolve reference input to get the original script if script ref is set. For this, utxo service is checked to get
     * the available utxo for the reference input.
     *
     * @param transaction
     * @return map of scriptHash and PlutusScript
     */
    public Map<String, PlutusScript> getScripts(Transaction transaction) {
        Map<String, PlutusScript> scriptsMap = new HashMap<>();
        //PlutusV1 scripts
        if (transaction.getWitnesses().getPlutusV1Scripts() != null
                && transaction.getWitnesses().getPlutusV1Scripts().size() > 0) {
            transaction.getWitnesses().getPlutusV1Scripts()
                    .forEach(script -> scriptsMap.put(ScriptUtil.getPlutusScriptHash(script), script));
        }

        //PlutusV2 scripts
        if (transaction.getWitnesses().getPlutusV2Scripts() != null
                && transaction.getWitnesses().getPlutusV2Scripts().size() > 0) {
            transaction.getWitnesses().getPlutusV2Scripts()
                    .forEach(script -> scriptsMap.put(ScriptUtil.getPlutusScriptHash(script), script));
        }

        //PlutusV3 scripts
        if (transaction.getWitnesses().getPlutusV3Scripts() != null
                && transaction.getWitnesses().getPlutusV3Scripts().size() > 0) {
            transaction.getWitnesses().getPlutusV3Scripts()
                    .forEach(script -> scriptsMap.put(ScriptUtil.getPlutusScriptHash(script), script));
        }

        //Check if reference input is there, then resolve it if script_ref found for the input
        if (transaction.getBody().getReferenceInputs() != null
                && transaction.getBody().getReferenceInputs().size() > 0) {
            transaction.getBody().getReferenceInputs()
                    .stream().map(transactionInput ->
                            utxoClient.getUtxoById(new UtxoKey(transactionInput.getTransactionId(), transactionInput.getIndex())))
                    .filter(Optional::isPresent)
                    .forEach(addressUtxoOptional -> {
                        AddressUtxo addressUtxo = addressUtxoOptional.get();
                        if (addressUtxo.getScriptRef() != null && !addressUtxo.getScriptRef().isEmpty()) {
                            var cclScript = ScriptReferenceUtil.deserializeScriptRef(HexUtil.decodeHexString(addressUtxo.getScriptRef()));
                            if (cclScript.getScriptType() != 0) {
                                PlutusScript plutusScript = ScriptUtil.deserializeScriptRef(addressUtxo);
                                if (plutusScript != null)
                                    scriptsMap.put(ScriptUtil.getPlutusScriptHash(plutusScript), plutusScript);
                                else {
                                    log.error("PlutusScript is null for tx: " + transaction.getTxHash());
                                }
                            }                         }
                    });
        }

        //Check if any of the input has script ref set
        transaction.getBody().getInputs()
                .stream().map(transactionInput ->
                        utxoClient.getUtxoById(new UtxoKey(transactionInput.getTransactionId(), transactionInput.getIndex())))
                .filter(Optional::isPresent)
                .forEach(addressUtxoOptional -> {
                    AddressUtxo addressUtxo = addressUtxoOptional.get();
                    if (addressUtxo.getScriptRef() != null && !addressUtxo.getScriptRef().isEmpty()) {
                        var cclScript = ScriptReferenceUtil.deserializeScriptRef(HexUtil.decodeHexString(addressUtxo.getScriptRef()));
                        if (cclScript.getScriptType() != 0) {
                            PlutusScript plutusScript = ScriptUtil.deserializeScriptRef(addressUtxo);
                            if (plutusScript != null)
                                scriptsMap.put(ScriptUtil.getPlutusScriptHash(plutusScript), plutusScript);
                            else {
                                log.error("PlutusScript is null for tx: " + transaction.getTxHash());
                            }
                        }                           }
                });

        return scriptsMap;
    }
}

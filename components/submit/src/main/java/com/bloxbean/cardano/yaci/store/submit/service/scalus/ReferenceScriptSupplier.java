package com.bloxbean.cardano.yaci.store.submit.service.scalus;

import com.bloxbean.cardano.client.plutus.spec.PlutusScript;
import com.bloxbean.cardano.client.spec.Script;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.util.ScriptReferenceUtil;
import lombok.extern.slf4j.Slf4j;
import scalus.bloxbean.ScriptSupplier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
class ReferenceScriptSupplier implements ScriptSupplier {
    private final Map<String, PlutusScript> scripts = new ConcurrentHashMap<>();

    void register(AddressUtxo addressUtxo) {
        if (addressUtxo == null)
            return;

        register(addressUtxo.getReferenceScriptHash(), addressUtxo.getScriptRef());
    }

    void register(String referenceScriptHash, String scriptRef) {
        if (isBlank(referenceScriptHash) || isBlank(scriptRef))
            return;

        try {
            Script script = ScriptReferenceUtil.deserializeScriptRef(HexUtil.decodeHexString(scriptRef));
            if (script instanceof PlutusScript plutusScript) {
                scripts.put(referenceScriptHash, plutusScript);
            } else if (log.isDebugEnabled()) {
                log.debug("Ignoring non-Plutus reference script for hash {}", referenceScriptHash);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to deserialize reference script for hash " + referenceScriptHash, e);
        }
    }

    void register(String referenceScriptHash, PlutusScript plutusScript) {
        if (isBlank(referenceScriptHash) || plutusScript == null)
            return;

        scripts.put(referenceScriptHash, plutusScript);
    }

    @Override
    public PlutusScript getScript(String scriptHash) {
        PlutusScript script = scripts.get(scriptHash);
        if (script == null)
            throw new IllegalArgumentException("Reference script not found for hash " + scriptHash);

        return script;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

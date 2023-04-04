package com.bloxbean.cardano.yaci.store.script.helper;

import com.bloxbean.cardano.yaci.core.model.PlutusScript;
import com.bloxbean.cardano.yaci.store.script.domain.ScriptType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScriptContext {
    private PlutusScript plutusScript;
    private String redeemer;
    private String datum;
    private String datumHash;

    public String getScriptHash() {
        if (plutusScript == null)
            return null;

        return ScriptUtil.getPlutusScriptHash(plutusScript);
    }

    public ScriptType getPlutusScriptType() {
        if (plutusScript == null)
            return null;

        return ScriptUtil.toPlutusScriptType(plutusScript.getType());
    }
}

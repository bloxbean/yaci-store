package com.bloxbean.cardano.yaci.store.script.helper;

import com.bloxbean.cardano.yaci.core.model.PlutusScript;
import com.bloxbean.cardano.yaci.core.model.Redeemer;
import com.bloxbean.cardano.yaci.store.script.domain.ScriptType;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ScriptContext {
    private PlutusScript plutusScript;
    private Redeemer redeemer;
    private String datum;
    private String datumHash;
    private String scriptHash;

    public ScriptContext(PlutusScript plutusScript, Redeemer redeemer, String datum, String datumHash) {
        this.plutusScript = plutusScript;
        this.redeemer = redeemer;
        this.datum = datum;
        this.datumHash = datumHash;
    }

    public String getScriptHash() {
        if (scriptHash != null || plutusScript == null)
            return scriptHash;

        return ScriptUtil.getPlutusScriptHash(plutusScript);
    }

    public ScriptType getPlutusScriptType() {
        if (plutusScript == null)
            return null;

        return ScriptUtil.toPlutusScriptType(plutusScript.getType());
    }
}

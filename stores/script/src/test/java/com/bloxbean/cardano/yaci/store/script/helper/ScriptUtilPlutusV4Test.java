package com.bloxbean.cardano.yaci.store.script.helper;

import com.bloxbean.cardano.yaci.core.model.PlutusScript;
import com.bloxbean.cardano.yaci.core.model.PlutusScriptType;
import com.bloxbean.cardano.yaci.core.util.HexUtil;
import com.bloxbean.cardano.yaci.store.common.util.ScriptReferenceUtil;
import com.bloxbean.cardano.yaci.store.script.domain.ScriptType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Plutus V4 (Dijkstra era) scripts. The script is the one in the Haskell ledger's w42 golden transaction
 * (cardano-ledger 9df9f98c, eras/dijkstra/impl/golden/tx.cbor), as Yaci reads it from witness set key 8.
 */
class ScriptUtilPlutusV4Test {
    //CBOR byte string of the script, as in PlutusScript.content for every Plutus version
    private static final String SCRIPT = "484701000022220011";
    //blake2b-224(0x04 || script bytes)
    private static final String V4_HASH = "a7dadad7b530cf88eba8be6670ba33edd4aafbc5a0ed3d6a1eb3dd89";
    //The same bytes as a Plutus V3 script hash differently: the language tag is part of the hash
    private static final String V3_HASH = "f630ed7a34084de8bd767d21f345c42e35b5a25165e5aba335c33766";

    @Test
    void witnessScriptHashUsesTheV4Tag() {
        assertThat(ScriptUtil.getPlutusScriptHash(new PlutusScript(PlutusScriptType.PlutusScriptV4, SCRIPT)))
                .isEqualTo(V4_HASH);
        assertThat(ScriptUtil.getPlutusScriptHash(new PlutusScript(PlutusScriptType.PlutusScriptV3, SCRIPT)))
                .isEqualTo(V3_HASH);
    }

    @Test
    void scriptType() {
        assertThat(ScriptUtil.toPlutusScriptType(PlutusScriptType.PlutusScriptV4)).isEqualTo(ScriptType.PLUTUS_V4);
    }

    @Test
    void referenceScriptBecomesAPlutusV4Script() {
        //script_ref content [4, bytes]: 82 04 48 4701000022220011
        var cclScript = ScriptReferenceUtil.deserializeScriptRef(HexUtil.decodeHexString("8204" + SCRIPT));

        PlutusScript plutusScript = ScriptUtil.toPlutusScript(cclScript);

        assertThat(plutusScript.getType()).isEqualTo(PlutusScriptType.PlutusScriptV4);
        assertThat(plutusScript.getContent()).isEqualTo(SCRIPT);
        //A redeemer finds the same script whether it is a witness or a reference script
        assertThat(ScriptUtil.getPlutusScriptHash(plutusScript)).isEqualTo(V4_HASH);
    }
}

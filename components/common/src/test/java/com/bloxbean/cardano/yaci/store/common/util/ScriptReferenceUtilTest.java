package com.bloxbean.cardano.yaci.store.common.util;

import com.bloxbean.cardano.client.spec.Script;
import com.bloxbean.cardano.yaci.core.util.HexUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScriptReferenceUtilTest {
    //Musashi (Dijkstra era): tx c095234678d98a74cd1488bda1971fbd2e9c828acf57f7673c679ec601abf7a6, output 0
    static final String PLUTUS_V4_SCRIPT_REF = "820458af58ad01010029800aba2aba1aba0aab9faab9eaab9dab9a4888888966002646464b30013370e900118029baa001899914c004c02c00a601660180052259800800c528456600266ebcc03400400e2946266004004601c002804100b4dd618059806180618061806180618061806180618061806180618041baa300b00548888cc008008004600c6ea8004cdd2a400066010601200497ae08b200830083009001300800130033754011149a26cac80081";
    //db-sync reference_script_hash for that output
    static final String PLUTUS_V4_SCRIPT_HASH = "603320d06682fcd9c54eee7af8566c4a2d7c75cc4692e4b14a1be7a4";

    @Test
    void plutusV4ScriptRef() throws Exception {
        Script script = ScriptReferenceUtil.deserializeScriptRef(HexUtil.decodeHexString(PLUTUS_V4_SCRIPT_REF));

        assertThat(script).isInstanceOf(PlutusV4Script.class);
        assertThat(script.getScriptType()).isEqualTo(4);
        assertThat(((PlutusV4Script) script).getCborHex()).isEqualTo(PLUTUS_V4_SCRIPT_REF.substring(4));
        assertThat(HexUtil.encodeHexString(script.scriptRefBytes())).isEqualTo(PLUTUS_V4_SCRIPT_REF);
        assertThat(ScriptReferenceUtil.getReferenceScriptHash(HexUtil.decodeHexString(PLUTUS_V4_SCRIPT_REF)))
                .isEqualTo(PLUTUS_V4_SCRIPT_HASH);
    }

    @Test
    void unknownScriptTypeIsRejected() {
        assertThatThrownBy(() -> ScriptReferenceUtil.deserializeScriptRef(HexUtil.decodeHexString("82054100")))
                .hasRootCauseMessage("Invalid type : 5");
    }
}

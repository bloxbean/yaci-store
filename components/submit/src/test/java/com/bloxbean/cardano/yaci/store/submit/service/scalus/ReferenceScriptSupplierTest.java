package com.bloxbean.cardano.yaci.store.submit.service.scalus;

import com.bloxbean.cardano.client.plutus.spec.PlutusV1Script;
import com.bloxbean.cardano.client.transaction.spec.script.ScriptPubkey;
import com.bloxbean.cardano.client.util.HexUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReferenceScriptSupplierTest {
    @Test
    void registerStoresPlutusReferenceScript() throws Exception {
        PlutusV1Script script = PlutusV1Script.builder()
                .cborHex("4e4d01000033222220051200120011")
                .build();
        String scriptHash = HexUtil.encodeHexString(script.getScriptHash());
        String scriptRef = HexUtil.encodeHexString(script.scriptRefBytes());
        ReferenceScriptSupplier supplier = new ReferenceScriptSupplier();

        supplier.register(scriptHash, scriptRef);

        var resolved = supplier.getScript(scriptHash);
        assertThat(resolved).isInstanceOf(PlutusV1Script.class);
        assertThat(HexUtil.encodeHexString(resolved.getScriptHash())).isEqualTo(scriptHash);
    }

    @Test
    void registerIgnoresNativeReferenceScript() throws Exception {
        ScriptPubkey nativeScript = new ScriptPubkey("00000000000000000000000000000000000000000000000000000000");
        String scriptRef = HexUtil.encodeHexString(nativeScript.scriptRefBytes());
        ReferenceScriptSupplier supplier = new ReferenceScriptSupplier();

        supplier.register("native-script-hash", scriptRef);

        assertThatThrownBy(() -> supplier.getScript("native-script-hash"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Reference script not found");
    }
}

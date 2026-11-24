package com.bloxbean.cardano.yaci.store.script.helper;

import com.bloxbean.cardano.client.spec.Era;
import com.bloxbean.cardano.yaci.core.model.RedeemerTag;
import com.bloxbean.cardano.yaci.core.model.serializers.TransactionBodySerializer;
import com.bloxbean.cardano.yaci.core.model.serializers.WitnessesSerializer;
import com.bloxbean.cardano.yaci.core.util.HexUtil;
import com.bloxbean.cardano.yaci.helper.model.Transaction;
import com.bloxbean.cardano.yaci.store.script.domain.ScriptType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GuardingRedeemerTest {
    // alwaysSucceeds @PlutusV4 3 from cardano-ledger 95b36905, eras/dijkstra/impl/golden/tx.cbor.
    private static final String V4_SCRIPT = "4701000022220011";
    private static final String V4_HASH = "a7dadad7b530cf88eba8be6670ba33edd4aafbc5a0ed3d6a1eb3dd89";
    private static final String OTHER_SCRIPT_HASH = "00".repeat(28);

    @Test
    void indexesTheFullGuardSetInWireOrder() {
        ScriptContext guard = match(1);
        assertThat(guard.getRedeemer().getTag()).isEqualTo(RedeemerTag.Guarding);
        assertThat(guard.getScriptHash()).isEqualTo(V4_HASH);
        assertThat(guard.getPlutusScriptType()).isEqualTo(ScriptType.PLUTUS_V4);
        assertThat(match(0).getScriptHash()).isNull(); // key guard
        assertThat(match(3).getScriptHash()).isNull(); // past the end
    }

    @Test
    void retainsAReferencedGuardHashWhenTheScriptBytesAreUnavailable() {
        ScriptContext guard = match(2);
        assertThat(guard.getScriptHash()).isEqualTo(OTHER_SCRIPT_HASH);
        assertThat(guard.getPlutusScriptType()).isNull();
    }

    private ScriptContext match(int index) {
        // Body key 14 is an ordered set: key credential, witnessed script, reference-only script.
        String body = "a40080018002000ed9010283"
                + "8200581c" + "ff".repeat(28)
                + "8201581c" + V4_HASH
                + "8201581c" + OTHER_SCRIPT_HASH;
        // Witness key 5 maps [Guarding, index] to [data, budget]; key 8 contains the V4 script.
        String witnesses = "a205a18206%02x820082010208d901028148".formatted(index) + V4_SCRIPT;
        var tx = Transaction.builder()
                .body(TransactionBodySerializer.INSTANCE.read(HexUtil.decodeHexString(body), Era.Dijkstra))
                .witnesses(WitnessesSerializer.INSTANCE.deserialize(HexUtil.decodeHexString(witnesses), Era.Dijkstra))
                .build();
        var scripts = new TxScriptFinder(null).getScripts(tx);
        return new RedeemerDatumMatcher(null).findScriptsForRedeemers(tx, scripts).getFirst();
    }
}

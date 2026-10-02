package com.bloxbean.cardano.yaci.store.script.helper;

import com.bloxbean.cardano.client.exception.CborRuntimeException;
import com.bloxbean.cardano.client.plutus.spec.PlutusData;
import com.bloxbean.cardano.yaci.core.model.Datum;
import com.bloxbean.cardano.yaci.core.model.NativeScript;
import com.bloxbean.cardano.yaci.core.model.PlutusScript;
import com.bloxbean.cardano.yaci.core.model.PlutusScriptType;
import com.bloxbean.cardano.yaci.core.util.HexUtil;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScriptUtilTest {
    //Small stack so that the recursive CCL path overflows deterministically
    private static final long STACK_SIZE = 256 * 1024;
    private static final int DEEP_NESTING = 100_000;

    @Test
    void getDatumHash_whenDatumIsDeeplyNested_shouldReturnNullInsteadOfStackOverflow() throws Exception {
        String deepDatum = deeplyNestedListDatum(DEEP_NESTING);

        //Make sure the CCL path really overflows in this setup
        assertThatThrownBy(() -> runWithSmallStack(() -> PlutusData.deserialize(HexUtil.decodeHexString(deepDatum)).getDatumHash()))
                .isInstanceOf(StackOverflowError.class);

        String datumHash = runWithSmallStack(() -> ScriptUtil.getDatumHash(deepDatum));

        assertThat(datumHash).isNull();
    }

    @Test
    void getDatumHash_whenDatumIsShallow_shouldReturnHash() throws Exception {
        String datum = deeplyNestedListDatum(3);
        String expected = PlutusData.deserialize(HexUtil.decodeHexString(datum)).getDatumHash();

        String datumHash = runWithSmallStack(() -> ScriptUtil.getDatumHash(datum));

        assertThat(datumHash).isEqualTo(expected);
    }

    @Test
    void getDatumHash_whenYaciDatumHasHash_shouldUseItWithoutDeserializing() throws Exception {
        String yaciHash = "923918e403bf43c34b4ef6b48eb2ee04babed17320d8d1b9ff9ad086e86f44ec";
        Datum datum = new Datum(yaciHash, deeplyNestedListDatum(DEEP_NESTING), null);

        String datumHash = runWithSmallStack(() -> ScriptUtil.getDatumHash(datum));

        assertThat(datumHash).isEqualTo(yaciHash);
    }

    @Test
    void getDatumHash_whenYaciDatumHasNoHash_shouldCalculateFromCbor() throws Exception {
        String cbor = deeplyNestedListDatum(3);
        String expected = PlutusData.deserialize(HexUtil.decodeHexString(cbor)).getDatumHash();
        Datum datum = new Datum(null, cbor, null);

        String datumHash = runWithSmallStack(() -> ScriptUtil.getDatumHash(datum));

        assertThat(datumHash).isEqualTo(expected);
    }

    @Test
    void getNativeScriptHash_whenScriptIsDeeplyNested_shouldThrowIllegalStateException() {
        NativeScript nativeScript = new NativeScript(1, deeplyNestedNativeScriptJson(DEEP_NESTING));

        assertThatThrownBy(() -> runWithSmallStack(() -> ScriptUtil.getNativeScriptHash(nativeScript)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void deserializeScriptRef_whenPlutusScriptRefIsValid_shouldReturnScript() throws Exception {
        // [2, h'480100002221200101'] (PlutusV2 always-succeeds, cardano-cli cborHex 49480100002221200101)
        String scriptRef = "820249480100002221200101";

        PlutusScript plutusScript = runWithSmallStack(() -> ScriptUtil.deserializeScriptRef(scriptRef));

        assertThat(plutusScript.getType()).isEqualTo(PlutusScriptType.PlutusScriptV2);
        assertThat(ScriptUtil.getPlutusScriptHash(plutusScript)).isEqualTo("3a888d65f16790950a72daee1f63aa05add6d268434107cfa5b67712");
    }

    @Test
    void deserializeScriptRef_whenScriptRefIsDeeplyNested_shouldThrowCborRuntimeException() {
        byte[] scriptRef = HexUtil.decodeHexString(deeplyNestedNativeScriptRef(DEEP_NESTING));

        assertThatThrownBy(() -> runWithSmallStack(() -> ScriptUtil.deserializeScriptRef(scriptRef)))
                .isInstanceOf(CborRuntimeException.class);
    }

    @Test
    void deserializeScriptRef_whenHexScriptRefIsDeeplyNested_shouldReturnNull() throws Exception {
        String scriptRef = deeplyNestedNativeScriptRef(DEEP_NESTING);

        PlutusScript plutusScript = runWithSmallStack(() -> ScriptUtil.deserializeScriptRef(scriptRef));

        assertThat(plutusScript).isNull();
    }

    /**
     * [0, ScriptAll[ScriptAll[ ... [ScriptPubkey] ... ]]], ScriptAll = [1, [scripts]]
     */
    private static String deeplyNestedNativeScriptRef(int depth) {
        return "8200" + "820181".repeat(depth) + "8200581cad7a7b87959173fc9eac9a85891cc93892f800dd45c0544128228884";
    }

    /**
     * List of list of ... of integer 0, encoded as definite length CBOR arrays (0x81 ... 0x81 0x00)
     */
    private static String deeplyNestedListDatum(int depth) {
        return "81".repeat(depth) + "00";
    }

    private static String deeplyNestedNativeScriptJson(int depth) {
        String sig = "{\"type\":\"sig\",\"keyHash\":\"ad7a7b87959173fc9eac9a85891cc93892f800dd45c0544128228884\"}";
        return "{\"type\":\"all\",\"scripts\":[".repeat(depth) + sig + "]}".repeat(depth);
    }

    private static <T> T runWithSmallStack(Callable<T> callable) throws Exception {
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        Thread thread = new Thread(null, () -> {
            try {
                result.set(callable.call());
            } catch (Throwable t) {
                error.set(t);
            }
        }, "small-stack", STACK_SIZE);
        thread.start();
        thread.join();

        Throwable t = error.get();
        if (t instanceof Exception e)
            throw e;
        if (t instanceof Error e)
            throw e;

        return result.get();
    }
}

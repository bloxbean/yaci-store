package com.bloxbean.cardano.yaci.store.common.util;

import com.bloxbean.cardano.client.common.cbor.CborSerializationUtil;
import com.bloxbean.cardano.client.exception.CborRuntimeException;
import com.bloxbean.cardano.yaci.core.util.HexUtil;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScriptReferenceUtilTest {
    //Small stack so that the recursive CCL path overflows deterministically
    private static final long STACK_SIZE = 256 * 1024;
    private static final int DEEP_NESTING = 100_000;

    // ScriptPubkey: [0, h'ad7a...8884']
    private static final String SIG_SCRIPT = "8200581cad7a7b87959173fc9eac9a85891cc93892f800dd45c0544128228884";

    @Test
    void getReferenceScriptHash_whenNativeScriptIsShallow_shouldReturnHash() throws Exception {
        // [0, native script]; hash = blake2b-224(0x00 || native script cbor)
        String scriptRef = "8200" + SIG_SCRIPT;

        String hash = runWithSmallStack(() -> ScriptReferenceUtil.getReferenceScriptHash(HexUtil.decodeHexString(scriptRef)));

        assertThat(hash).isEqualTo("b9bd3fb4511908402fbef848eece773bb44c867c25ac8c08d9ec3313");
    }

    @Test
    void deserializeScriptRef_whenNativeScriptIsDeeplyNested_shouldThrowCborRuntimeException() {
        byte[] scriptRef = HexUtil.decodeHexString(deeplyNestedNativeScriptRef(DEEP_NESTING));

        //Make sure the CCL path really overflows in this setup
        assertThatThrownBy(() -> runWithSmallStack(() -> CborSerializationUtil.deserialize(scriptRef)))
                .isInstanceOf(StackOverflowError.class);

        assertThatThrownBy(() -> runWithSmallStack(() -> ScriptReferenceUtil.deserializeScriptRef(scriptRef)))
                .isInstanceOf(CborRuntimeException.class);
    }

    @Test
    void getReferenceScriptHash_whenNativeScriptIsDeeplyNested_shouldThrowCborRuntimeException() {
        byte[] scriptRef = HexUtil.decodeHexString(deeplyNestedNativeScriptRef(DEEP_NESTING));

        assertThatThrownBy(() -> runWithSmallStack(() -> ScriptReferenceUtil.getReferenceScriptHash(scriptRef)))
                .isInstanceOf(CborRuntimeException.class);
    }

    /**
     * [0, ScriptAll[ScriptAll[ ... [ScriptPubkey] ... ]]], ScriptAll = [1, [scripts]]
     */
    private static String deeplyNestedNativeScriptRef(int depth) {
        return "8200" + "820181".repeat(depth) + SIG_SCRIPT;
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

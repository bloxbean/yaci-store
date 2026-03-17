package com.bloxbean.cardano.yaci.store.epochnonce.util;

import com.bloxbean.cardano.client.crypto.Blake2bUtil;
import com.bloxbean.cardano.client.util.HexUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NonceUtilTest {

    // --- combineNonces tests ---

    @Test
    void combineNonces_bothNull_returnsNull() {
        assertThat(NonceUtil.combineNonces(null, null)).isNull();
    }

    @Test
    void combineNonces_firstNull_returnsSecond() {
        byte[] b = new byte[]{0x01, 0x02, 0x03};
        assertThat(NonceUtil.combineNonces(null, b)).isSameAs(b);
    }

    @Test
    void combineNonces_secondNull_returnsFirst() {
        byte[] a = new byte[]{0x04, 0x05, 0x06};
        assertThat(NonceUtil.combineNonces(a, null)).isSameAs(a);
    }

    @Test
    void combineNonces_bothPresent_returnsBlake2bOfConcatenation() {
        byte[] a = new byte[32];  // 32 zero bytes
        byte[] b = new byte[32];  // 32 zero bytes

        byte[] expected = Blake2bUtil.blake2bHash256(new byte[64]); // blake2b(64 zero bytes)
        byte[] result = NonceUtil.combineNonces(a, b);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void combineNonces_isNotCommutative() {
        byte[] a = HexUtil.decodeHexString("0000000000000000000000000000000000000000000000000000000000000001");
        byte[] b = HexUtil.decodeHexString("0000000000000000000000000000000000000000000000000000000000000002");

        byte[] ab = NonceUtil.combineNonces(a, b);
        byte[] ba = NonceUtil.combineNonces(b, a);

        // combine is NOT commutative because concat order matters
        assertThat(ab).isNotEqualTo(ba);
    }

    @Test
    void combineNonces_deterministicForSameInputs() {
        byte[] a = HexUtil.decodeHexString("abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890");
        byte[] b = HexUtil.decodeHexString("1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef");

        byte[] result1 = NonceUtil.combineNonces(a, b);
        byte[] result2 = NonceUtil.combineNonces(a, b);

        assertThat(result1).isEqualTo(result2);
    }

    @Test
    void combineNonces_outputIs32Bytes() {
        byte[] a = new byte[32];
        byte[] b = new byte[32];

        byte[] result = NonceUtil.combineNonces(a, b);

        assertThat(result).hasSize(32);
    }

    // --- vrfNonceValue tests ---

    @Test
    void vrfNonceValue_outputIs32Bytes() {
        byte[] vrfOutput = new byte[64]; // typical VRF output size
        byte[] result = NonceUtil.vrfNonceValue(vrfOutput);

        assertThat(result).hasSize(32);
    }

    @Test
    void vrfNonceValue_isDeterministic() {
        byte[] vrfOutput = HexUtil.decodeHexString(
                "720121d750ea77c65e393c78012df784ec79231ab7b81c8eb7c31b3ca393367f" +
                "0ce8f020a727156e05ecf8c9cd52c3bbbd3309832cd32fd53379a1f420ea6bc9");

        byte[] result1 = NonceUtil.vrfNonceValue(vrfOutput);
        byte[] result2 = NonceUtil.vrfNonceValue(vrfOutput);

        assertThat(result1).isEqualTo(result2);
    }

    @Test
    void vrfNonceValue_matchesExpectedComputation() {
        // Use a known VRF output from mainnet block 11043370
        byte[] vrfOutput = HexUtil.decodeHexString(
                "720121d750ea77c65e393c78012df784ec79231ab7b81c8eb7c31b3ca393367f" +
                "0ce8f020a727156e05ecf8c9cd52c3bbbd3309832cd32fd53379a1f420ea6bc9");

        // Manually compute expected: Blake2b_256(Blake2b_256("N" || vrfOutput))
        byte[] prefixed = new byte[1 + vrfOutput.length];
        prefixed[0] = (byte) 'N';
        System.arraycopy(vrfOutput, 0, prefixed, 1, vrfOutput.length);
        byte[] firstHash = Blake2bUtil.blake2bHash256(prefixed);
        byte[] expected = Blake2bUtil.blake2bHash256(firstHash);

        byte[] result = NonceUtil.vrfNonceValue(vrfOutput);

        assertThat(result).isEqualTo(expected);
        assertThat(HexUtil.encodeHexString(result)).hasSize(64); // 32 bytes = 64 hex chars
    }

    @Test
    void vrfNonceValue_differentInputsProduceDifferentOutputs() {
        byte[] vrf1 = HexUtil.decodeHexString(
                "720121d750ea77c65e393c78012df784ec79231ab7b81c8eb7c31b3ca393367f" +
                "0ce8f020a727156e05ecf8c9cd52c3bbbd3309832cd32fd53379a1f420ea6bc9");
        byte[] vrf2 = HexUtil.decodeHexString(
                "ff9af59c9b0bd179b38a112565ee94dc4972c0b773099a6eb1121f0981a85cd3" +
                "4f120caa0080e3a652ce6c6b1944a9709d908a7d0e92a2a9823ec6dadcde4b0f");

        byte[] result1 = NonceUtil.vrfNonceValue(vrf1);
        byte[] result2 = NonceUtil.vrfNonceValue(vrf2);

        assertThat(result1).isNotEqualTo(result2);
    }

    // --- vrfNonceValueTPraos tests ---

    @Test
    void vrfNonceValueTPraos_outputIs32Bytes() {
        byte[] vrfOutput = new byte[64]; // typical VRF output size
        byte[] result = NonceUtil.vrfNonceValueTPraos(vrfOutput);

        assertThat(result).hasSize(32);
    }

    @Test
    void vrfNonceValueTPraos_isSingleHash_noPrefix() {
        byte[] vrfOutput = HexUtil.decodeHexString(
                "720121d750ea77c65e393c78012df784ec79231ab7b81c8eb7c31b3ca393367f" +
                "0ce8f020a727156e05ecf8c9cd52c3bbbd3309832cd32fd53379a1f420ea6bc9");

        // TPraos: eta = Blake2b_256(vrfOutput) — single hash, no prefix
        byte[] expected = Blake2bUtil.blake2bHash256(vrfOutput);
        byte[] result = NonceUtil.vrfNonceValueTPraos(vrfOutput);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void vrfNonceValueTPraos_differsFromPraos() {
        byte[] vrfOutput = HexUtil.decodeHexString(
                "720121d750ea77c65e393c78012df784ec79231ab7b81c8eb7c31b3ca393367f" +
                "0ce8f020a727156e05ecf8c9cd52c3bbbd3309832cd32fd53379a1f420ea6bc9");

        byte[] tpraosResult = NonceUtil.vrfNonceValueTPraos(vrfOutput);
        byte[] praosResult = NonceUtil.vrfNonceValue(vrfOutput);

        // TPraos and Praos should produce different nonce values
        assertThat(tpraosResult).isNotEqualTo(praosResult);
    }

    // --- prevHashToNonce tests ---

    @Test
    void prevHashToNonce_nullInput_returnsNull() {
        assertThat(NonceUtil.prevHashToNonce(null)).isNull();
    }

    @Test
    void prevHashToNonce_emptyInput_returnsNull() {
        assertThat(NonceUtil.prevHashToNonce("")).isNull();
    }

    @Test
    void prevHashToNonce_validHex_returnsDecodedBytes() {
        String prevHash = "0c37ee61d1d71b2896c4427d95fa8b8dff02c6b72c8c225b64aced830865c0c6";
        byte[] result = NonceUtil.prevHashToNonce(prevHash);

        assertThat(result).isEqualTo(HexUtil.decodeHexString(prevHash));
        assertThat(result).hasSize(32);
    }

    @Test
    void prevHashToNonce_doesNotRehash() {
        // prevHashToNonce should be raw decode, NOT blake2b
        String prevHash = "0c37ee61d1d71b2896c4427d95fa8b8dff02c6b72c8c225b64aced830865c0c6";
        byte[] result = NonceUtil.prevHashToNonce(prevHash);
        byte[] hashed = Blake2bUtil.blake2bHash256(HexUtil.decodeHexString(prevHash));

        // Result should NOT equal the hashed value — it's raw bytes
        assertThat(result).isNotEqualTo(hashed);
    }

    // --- Integration: nonce evolution sequence ---

    @Test
    void nonceEvolution_threeBlockSequence_producesConsistentResult() {
        // Simulate evolving nonce through 3 blocks using real VRF outputs from mainnet
        String[] vrfOutputs = {
                "c18a5fa01c9149d984fec409ad7e14ad99ef9d2f2d1ec83dd8cf29c93cef61c7" +
                "96015f121b8f82ba6e6ad841ac3316cccf291b9716ad6f712778a8b3aafc269d",
                "ff9af59c9b0bd179b38a112565ee94dc4972c0b773099a6eb1121f0981a85cd3" +
                "4f120caa0080e3a652ce6c6b1944a9709d908a7d0e92a2a9823ec6dadcde4b0f",
                "720121d750ea77c65e393c78012df784ec79231ab7b81c8eb7c31b3ca393367f" +
                "0ce8f020a727156e05ecf8c9cd52c3bbbd3309832cd32fd53379a1f420ea6bc9"
        };

        byte[] evolvingNonce = new byte[32]; // start with zeros (genesis-like)

        for (String vrfHex : vrfOutputs) {
            byte[] vrfBytes = HexUtil.decodeHexString(vrfHex);
            byte[] eta = NonceUtil.vrfNonceValue(vrfBytes);
            evolvingNonce = NonceUtil.combineNonces(evolvingNonce, eta);
        }

        assertThat(evolvingNonce).isNotNull();
        assertThat(evolvingNonce).hasSize(32);

        // Run again — should produce the same result (deterministic)
        byte[] evolvingNonce2 = new byte[32];
        for (String vrfHex : vrfOutputs) {
            byte[] vrfBytes = HexUtil.decodeHexString(vrfHex);
            byte[] eta = NonceUtil.vrfNonceValue(vrfBytes);
            evolvingNonce2 = NonceUtil.combineNonces(evolvingNonce2, eta);
        }

        assertThat(evolvingNonce).isEqualTo(evolvingNonce2);
    }
}

package com.bloxbean.cardano.yaci.store.transaction.processor;

import co.nstant.in.cbor.model.DataItem;
import com.bloxbean.cardano.client.common.cbor.CborSerializationUtil;
import com.bloxbean.cardano.client.metadata.cbor.CBORMetadata;
import com.bloxbean.cardano.client.transaction.spec.Transaction;
import com.bloxbean.cardano.client.transaction.spec.TransactionInput;
import com.bloxbean.cardano.client.transaction.spec.TransactionOutput;
import com.bloxbean.cardano.client.transaction.spec.Value;
import com.bloxbean.cardano.yaci.core.model.AuxData;
import com.bloxbean.cardano.yaci.core.model.BootstrapWitness;
import com.bloxbean.cardano.yaci.core.model.VkeyWitness;
import com.bloxbean.cardano.yaci.core.model.Witnesses;
import com.bloxbean.cardano.yaci.core.util.HexUtil;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FullTxCborReassemblerTest {

    private static final String INPUT_TX_HASH = "9f8e77293350ba62c88bb1ee1633912a3e64950be96fb6b1613e421b52c6971d";
    private static final String OUTPUT_ADDRESS = "addr_test1vpfwv0ezc5g8a4mkku8hhy3y3vp92t7s3ul8g778g5yegsgalc6gc";
    // 32-byte vkey / 64-byte signature -- structurally valid hex lengths (not real key material;
    // this test exercises CBOR reassembly/round-tripping, not signature verification).
    private static final String VKEY_HEX = "0123456789abcdef".repeat(4);
    private static final String SIGNATURE_HEX = "0123456789abcdef".repeat(8);

    // Build a minimal, real (not fabricated) CCL transaction body and serialize it, exactly
    // like an actual block-producer would -- this is the same body CBOR shape yaci-store
    // stores today via transaction.getBody().getCbor().
    private static byte[] buildBodyCborBytes() throws Exception {
        var cclBody = com.bloxbean.cardano.client.transaction.spec.TransactionBody.builder()
                .inputs(List.of(new TransactionInput(INPUT_TX_HASH, 0)))
                .outputs(List.of(new TransactionOutput(OUTPUT_ADDRESS, Value.builder().coin(BigInteger.valueOf(5_000_000)).build())))
                .fee(BigInteger.valueOf(170_000))
                .build();
        DataItem bodyDataItem = cclBody.serialize();
        return CborSerializationUtil.serialize(bodyDataItem);
    }

    @Test
    void reassemble_roundTripsBodyAndVkeyWitness_throughCclTransactionDeserialize() throws Exception {
        byte[] bodyCborBytes = buildBodyCborBytes();
        String bodyCborHex = HexUtil.encodeHexString(bodyCborBytes);

        var yaciBody = com.bloxbean.cardano.yaci.core.model.TransactionBody.builder()
                .cbor(bodyCborHex)
                .build();

        // one real vkey witness (structurally valid hex; not a real signature, but this test only
        // exercises the CBOR reassembly/round-trip, not signature verification)
        VkeyWitness yaciVkeyWitness = VkeyWitness.builder()
                .key(VKEY_HEX)
                .signature(SIGNATURE_HEX)
                .build();
        Witnesses witnesses = Witnesses.builder()
                .vkeyWitnesses(List.of(yaciVkeyWitness))
                .build();

        com.bloxbean.cardano.yaci.helper.model.Transaction tx = com.bloxbean.cardano.yaci.helper.model.Transaction.builder()
                .txHash("dummyTxHash")
                .body(yaciBody)
                .witnesses(witnesses)
                .invalid(false)
                .build();

        byte[] fullTxCbor = FullTxCborReassembler.reassemble(tx);

        assertThat(fullTxCbor).isNotEmpty();

        // Round-trip through CCL's own deserializer -- if this parses, the bytes are a genuinely
        // valid full transaction (CBOR array, tag 84...), not just body-only bytes with extra noise.
        Transaction roundTripped = Transaction.deserialize(fullTxCbor);

        assertThat(roundTripped.isValid()).isTrue();
        assertThat(roundTripped.getBody().getInputs()).hasSize(1);
        assertThat(roundTripped.getBody().getInputs().get(0).getTransactionId()).isEqualTo(INPUT_TX_HASH);
        assertThat(roundTripped.getBody().getOutputs()).hasSize(1);
        assertThat(roundTripped.getBody().getFee()).isEqualTo(BigInteger.valueOf(170_000));

        assertThat(roundTripped.getWitnessSet().getVkeyWitnesses()).hasSize(1);
        assertThat(HexUtil.encodeHexString(roundTripped.getWitnessSet().getVkeyWitnesses().get(0).getVkey()))
                .isEqualTo(yaciVkeyWitness.getKey());
        assertThat(HexUtil.encodeHexString(roundTripped.getWitnessSet().getVkeyWitnesses().get(0).getSignature()))
                .isEqualTo(yaciVkeyWitness.getSignature());

        // The body must be preserved VERBATIM -- byte-for-byte identical to the original body CBOR --
        // never re-serialized through the CCL TransactionBody model (which may reshape/drop fields it
        // doesn't model). Locate the body element directly at the raw CBOR-array level to prove this,
        // independent of whatever CCL's own TransactionBody model chooses to expose.
        DataItem fullTxArrayItem = com.bloxbean.cardano.yaci.core.util.CborSerializationUtil.deserializeOne(fullTxCbor);
        assertThat(fullTxArrayItem).isInstanceOf(co.nstant.in.cbor.model.Array.class);
        co.nstant.in.cbor.model.DataItem reassembledBodyItem =
                ((co.nstant.in.cbor.model.Array) fullTxArrayItem).getDataItems().get(0);
        byte[] reassembledBodyBytes = com.bloxbean.cardano.yaci.core.util.CborSerializationUtil.serialize(reassembledBodyItem);
        assertThat(reassembledBodyBytes).isEqualTo(bodyCborBytes);
    }

    @Test
    void reassemble_mapsBootstrapWitness_intoWitnessSet() throws Exception {
        String bodyCborHex = HexUtil.encodeHexString(buildBodyCborBytes());
        var yaciBody = com.bloxbean.cardano.yaci.core.model.TransactionBody.builder()
                .cbor(bodyCborHex)
                .build();

        // structurally valid hex lengths for a Byron bootstrap witness (not real key material)
        String publicKeyHex = "ab".repeat(32);
        String signatureHex = "cd".repeat(64);
        String chainCodeHex = "ef".repeat(32);
        String attributesHex = "a0"; // CBOR encoding of an empty map -- a common Byron attributes payload

        BootstrapWitness yaciBootstrapWitness = BootstrapWitness.builder()
                .publicKey(publicKeyHex)
                .signature(signatureHex)
                .chainCode(chainCodeHex)
                .attributes(attributesHex)
                .build();

        Witnesses witnesses = Witnesses.builder()
                .bootstrapWitnesses(List.of(yaciBootstrapWitness))
                .build();

        com.bloxbean.cardano.yaci.helper.model.Transaction tx = com.bloxbean.cardano.yaci.helper.model.Transaction.builder()
                .txHash("dummyTxHashBootstrap")
                .body(yaciBody)
                .witnesses(witnesses)
                .invalid(false)
                .build();

        byte[] fullTxCbor = FullTxCborReassembler.reassemble(tx);
        Transaction roundTripped = Transaction.deserialize(fullTxCbor);

        assertThat(roundTripped.getWitnessSet().getBootstrapWitnesses()).hasSize(1);
        var bootstrapWitness = roundTripped.getWitnessSet().getBootstrapWitnesses().get(0);
        assertThat(HexUtil.encodeHexString(bootstrapWitness.getPublicKey())).isEqualTo(publicKeyHex);
        assertThat(HexUtil.encodeHexString(bootstrapWitness.getSignature())).isEqualTo(signatureHex);
        assertThat(HexUtil.encodeHexString(bootstrapWitness.getChainCode())).isEqualTo(chainCodeHex);
        assertThat(HexUtil.encodeHexString(bootstrapWitness.getAttributes())).isEqualTo(attributesHex);
    }

    @Test
    void reassemble_preservesAuxiliaryDataScripts_notJustMetadata() throws Exception {
        String bodyCborHex = HexUtil.encodeHexString(buildBodyCborBytes());
        var yaciBody = com.bloxbean.cardano.yaci.core.model.TransactionBody.builder()
                .cbor(bodyCborHex)
                .build();

        CBORMetadata cclMetadata = new CBORMetadata();
        cclMetadata.put(BigInteger.valueOf(674), "aux-test");
        String metadataCborHex = HexUtil.encodeHexString(cclMetadata.serialize());

        String keyHash = "a".repeat(56);
        var yaciNativeScript = com.bloxbean.cardano.yaci.core.model.NativeScript.builder()
                .type(0)
                .content("{\"type\":\"sig\",\"keyHash\":\"" + keyHash + "\"}")
                .build();

        // A PlutusV2 script attached via auxiliary data (content = CBOR bytestring wrapping the raw
        // 7-byte script), exercising the plutus path of the aux-script fix, not just native scripts.
        var yaciAuxPlutusV2 = com.bloxbean.cardano.yaci.core.model.PlutusScript.builder()
                .type(com.bloxbean.cardano.yaci.core.model.PlutusScriptType.PlutusScriptV2)
                .content("4746010000220011")
                .build();

        AuxData auxData = new AuxData(metadataCborHex, null, List.of(yaciNativeScript), null, List.of(yaciAuxPlutusV2), null);

        com.bloxbean.cardano.yaci.helper.model.Transaction tx = com.bloxbean.cardano.yaci.helper.model.Transaction.builder()
                .txHash("dummyTxHashAux")
                .body(yaciBody)
                .auxData(auxData)
                .invalid(false)
                .build();

        byte[] fullTxCbor = FullTxCborReassembler.reassemble(tx);
        Transaction roundTripped = Transaction.deserialize(fullTxCbor);

        assertThat(roundTripped.getAuxiliaryData()).isNotNull();
        assertThat(roundTripped.getAuxiliaryData().getMetadata()).isNotNull();

        // The bug being fixed: aux scripts (native/PlutusV1-3) used to be silently dropped -- only
        // metadata was ever mapped. Assert the native script attached via auxiliary data survives too.
        assertThat(roundTripped.getAuxiliaryData().getNativeScripts()).hasSize(1);
        assertThat(roundTripped.getAuxiliaryData().getPlutusV2Scripts()).hasSize(1);
        var nativeScript = roundTripped.getAuxiliaryData().getNativeScripts().get(0);
        assertThat(nativeScript).isInstanceOf(com.bloxbean.cardano.client.transaction.spec.script.ScriptPubkey.class);
        assertThat(((com.bloxbean.cardano.client.transaction.spec.script.ScriptPubkey) nativeScript).getKeyHash())
                .isEqualTo(keyHash);
    }

    @Test
    void reassemble_throws_whenBodyCborMissing() {
        com.bloxbean.cardano.yaci.helper.model.Transaction tx = com.bloxbean.cardano.yaci.helper.model.Transaction.builder()
                .txHash("dummyTxHash")
                .body(com.bloxbean.cardano.yaci.core.model.TransactionBody.builder().build())
                .invalid(false)
                .build();

        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> FullTxCborReassembler.reassemble(tx));
    }
}

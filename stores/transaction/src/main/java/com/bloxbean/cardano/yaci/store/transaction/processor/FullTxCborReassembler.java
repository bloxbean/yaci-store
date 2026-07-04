package com.bloxbean.cardano.yaci.store.transaction.processor;

import co.nstant.in.cbor.model.Array;
import co.nstant.in.cbor.model.ByteString;
import co.nstant.in.cbor.model.DataItem;
import co.nstant.in.cbor.model.Map;
import co.nstant.in.cbor.model.SimpleValue;
import com.bloxbean.cardano.client.metadata.cbor.CBORMetadata;
import com.bloxbean.cardano.client.plutus.spec.PlutusData;
import com.bloxbean.cardano.client.plutus.spec.PlutusV1Script;
import com.bloxbean.cardano.client.plutus.spec.PlutusV2Script;
import com.bloxbean.cardano.client.plutus.spec.PlutusV3Script;
import com.bloxbean.cardano.client.plutus.spec.Redeemer;
import com.bloxbean.cardano.client.spec.Era;
import com.bloxbean.cardano.client.transaction.spec.AuxiliaryData;
import com.bloxbean.cardano.client.transaction.spec.BootstrapWitness;
import com.bloxbean.cardano.client.transaction.spec.TransactionWitnessSet;
import com.bloxbean.cardano.client.transaction.spec.VkeyWitness;
import com.bloxbean.cardano.client.transaction.spec.script.NativeScript;
import com.bloxbean.cardano.yaci.core.model.PlutusScript;
import com.bloxbean.cardano.yaci.core.util.CborSerializationUtil;
import com.bloxbean.cardano.yaci.core.util.HexUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Reassembles the FULL signed transaction CBOR (body + witness set + isValid + auxiliary data --
 * a CBOR array, leading byte {@code 0x84...}) from a yaci-helper {@link com.bloxbean.cardano.yaci.helper.model.Transaction},
 * whose {@code getBody().getCbor()} only contains the transaction BODY (a CBOR map, leading byte {@code 0xa4...}).
 * <p>
 * The transaction body is mandatory: any failure deserializing it is unrecoverable and propagates to the
 * caller, which is expected to fall back to storing body-only CBOR (see
 * {@link TransactionProcessor#collectTransactionCbor}). Every other component (witnesses, scripts,
 * redeemers, datums, auxiliary data/metadata) is best-effort: a single malformed/unmappable item is
 * skipped (logged at debug) rather than aborting the whole reassembly, so the caller still gets a valid
 * {@code 0x84...} transaction with whatever detail could be recovered.
 * <p>
 * The body is never re-serialized through the CCL {@code TransactionBody} model: it is decoded once into a
 * raw {@code co.nstant.in.cbor} {@link DataItem} and placed into the transaction array verbatim, byte-for-byte
 * identical to what the block producer originally signed. This avoids CCL reshaping/dropping body fields it
 * doesn't (yet) model.
 */
@Slf4j
public class FullTxCborReassembler {

    private FullTxCborReassembler() {
    }

    /**
     * @param tx the yaci-helper transaction model for a single processed transaction
     * @return the full signed transaction, CBOR-serialized (array, leading byte {@code 0x84...})
     * @throws Exception if the mandatory transaction body cannot be deserialized
     */
    public static byte[] reassemble(com.bloxbean.cardano.yaci.helper.model.Transaction tx) throws Exception {
        DataItem bodyDataItem = deserializeBody(tx);
        TransactionWitnessSet witnessSet = buildWitnessSet(tx);
        AuxiliaryData auxiliaryData = buildAuxiliaryData(tx);

        DataItem witnessSetDataItem;
        try {
            witnessSetDataItem = witnessSet.serialize(Era.Conway);
        } catch (Exception e) {
            log.debug("Unable to serialize witness set for tx {}. Falling back to empty witness set: {}", tx.getTxHash(), e.getMessage());
            witnessSetDataItem = new Map();
        }

        DataItem auxDataItem = SimpleValue.NULL;
        if (auxiliaryData != null) {
            try {
                auxDataItem = auxiliaryData.serialize(Era.Conway);
            } catch (Exception e) {
                log.debug("Unable to serialize auxiliary data for tx {}. Dropping auxiliary data: {}", tx.getTxHash(), e.getMessage());
                auxDataItem = SimpleValue.NULL;
            }
        }

        // Cardano tx envelope, exact array order: [ body, witness_set, is_valid, auxiliary_data / null ]
        Array txArray = new Array();
        txArray.add(bodyDataItem);
        txArray.add(witnessSetDataItem);
        txArray.add(!tx.isInvalid() ? SimpleValue.TRUE : SimpleValue.FALSE);
        txArray.add(auxDataItem);

        return CborSerializationUtil.serialize(txArray);
    }

    /**
     * Mandatory -- any failure here is unrecoverable and propagates to the caller. Returns the ORIGINAL body
     * bytes decoded once as a raw CBOR map {@link DataItem} -- never re-serialized through the CCL
     * {@code TransactionBody} model -- so the assembled transaction preserves the body verbatim, byte-for-byte.
     */
    private static DataItem deserializeBody(com.bloxbean.cardano.yaci.helper.model.Transaction tx) {
        String bodyCborHex = tx.getBody() != null ? tx.getBody().getCbor() : null;
        if (bodyCborHex == null || bodyCborHex.isEmpty()) {
            throw new IllegalStateException("No body CBOR available for transaction " + tx.getTxHash()
                    + " (YaciConfig.INSTANCE.setReturnTxBodyCbor(true) may not be set)");
        }

        DataItem bodyDataItem = CborSerializationUtil.deserializeOne(HexUtil.decodeHexString(bodyCborHex));
        if (!(bodyDataItem instanceof Map)) {
            throw new IllegalStateException("Transaction body CBOR for " + tx.getTxHash() + " is not a CBOR map");
        }
        return bodyDataItem;
    }

    private static TransactionWitnessSet buildWitnessSet(com.bloxbean.cardano.yaci.helper.model.Transaction tx) {
        TransactionWitnessSet.TransactionWitnessSetBuilder builder = TransactionWitnessSet.builder();

        var witnesses = tx.getWitnesses();
        if (witnesses == null) {
            return builder.build();
        }

        List<VkeyWitness> vkeyWitnesses = new ArrayList<>();
        if (witnesses.getVkeyWitnesses() != null) {
            for (var vkeyWitness : witnesses.getVkeyWitnesses()) {
                try {
                    vkeyWitnesses.add(new VkeyWitness(
                            HexUtil.decodeHexString(vkeyWitness.getKey()),
                            HexUtil.decodeHexString(vkeyWitness.getSignature())));
                } catch (Exception e) {
                    log.debug("Skipping malformed vkey witness for tx {}: {}", tx.getTxHash(), e.getMessage());
                }
            }
        }
        if (!vkeyWitnesses.isEmpty()) {
            builder.vkeyWitnesses(vkeyWitnesses);
        }

        List<BootstrapWitness> bootstrapWitnesses = new ArrayList<>();
        if (witnesses.getBootstrapWitnesses() != null) {
            for (var bootstrapWitness : witnesses.getBootstrapWitnesses()) {
                try {
                    bootstrapWitnesses.add(BootstrapWitness.builder()
                            .publicKey(HexUtil.decodeHexString(bootstrapWitness.getPublicKey()))
                            .signature(HexUtil.decodeHexString(bootstrapWitness.getSignature()))
                            .chainCode(HexUtil.decodeHexString(bootstrapWitness.getChainCode()))
                            .attributes(HexUtil.decodeHexString(bootstrapWitness.getAttributes()))
                            .build());
                } catch (Exception e) {
                    log.debug("Skipping malformed bootstrap witness for tx {}: {}", tx.getTxHash(), e.getMessage());
                }
            }
        }
        if (!bootstrapWitnesses.isEmpty()) {
            builder.bootstrapWitnesses(bootstrapWitnesses);
        }

        List<PlutusV1Script> plutusV1Scripts = new ArrayList<>();
        if (witnesses.getPlutusV1Scripts() != null) {
            for (PlutusScript plutusScript : witnesses.getPlutusV1Scripts()) {
                try {
                    plutusV1Scripts.add(PlutusV1Script.deserialize(decodeAsByteString(plutusScript.getContent())));
                } catch (Exception e) {
                    log.debug("Skipping malformed PlutusV1 script for tx {}: {}", tx.getTxHash(), e.getMessage());
                }
            }
        }
        if (!plutusV1Scripts.isEmpty()) {
            builder.plutusV1Scripts(plutusV1Scripts);
        }

        List<PlutusV2Script> plutusV2Scripts = new ArrayList<>();
        if (witnesses.getPlutusV2Scripts() != null) {
            for (PlutusScript plutusScript : witnesses.getPlutusV2Scripts()) {
                try {
                    plutusV2Scripts.add(PlutusV2Script.deserialize(decodeAsByteString(plutusScript.getContent())));
                } catch (Exception e) {
                    log.debug("Skipping malformed PlutusV2 script for tx {}: {}", tx.getTxHash(), e.getMessage());
                }
            }
        }
        if (!plutusV2Scripts.isEmpty()) {
            builder.plutusV2Scripts(plutusV2Scripts);
        }

        List<PlutusV3Script> plutusV3Scripts = new ArrayList<>();
        if (witnesses.getPlutusV3Scripts() != null) {
            for (PlutusScript plutusScript : witnesses.getPlutusV3Scripts()) {
                try {
                    plutusV3Scripts.add(PlutusV3Script.deserialize(decodeAsByteString(plutusScript.getContent())));
                } catch (Exception e) {
                    log.debug("Skipping malformed PlutusV3 script for tx {}: {}", tx.getTxHash(), e.getMessage());
                }
            }
        }
        if (!plutusV3Scripts.isEmpty()) {
            builder.plutusV3Scripts(plutusV3Scripts);
        }

        // Native script JSON shape may not map cleanly onto CCL's NativeScript -- skip silently per-item on failure.
        List<NativeScript> nativeScripts = new ArrayList<>();
        if (witnesses.getNativeScripts() != null) {
            for (var nativeScript : witnesses.getNativeScripts()) {
                try {
                    nativeScripts.add(NativeScript.deserializeJson(nativeScript.getContent()));
                } catch (Exception e) {
                    log.debug("Skipping native script for tx {}: {}", tx.getTxHash(), e.getMessage());
                }
            }
        }
        if (!nativeScripts.isEmpty()) {
            builder.nativeScripts(nativeScripts);
        }

        List<Redeemer> redeemers = new ArrayList<>();
        if (witnesses.getRedeemers() != null) {
            for (var redeemer : witnesses.getRedeemers()) {
                try {
                    // yaci-core's per-redeemer Redeemer#getCbor() is the legacy pre-Conway [tag, index, data, exUnits]
                    // array shape (one entry, not the Conway map-keyed form) -- deserializePreConway is the
                    // non-deprecated call for that shape (Redeemer#deserialize(Array) just forwards to it).
                    DataItem redeemerDataItem = CborSerializationUtil.deserializeOne(HexUtil.decodeHexString(redeemer.getCbor()));
                    redeemers.add(Redeemer.deserializePreConway((Array) redeemerDataItem));
                } catch (Exception e) {
                    log.debug("Skipping malformed redeemer for tx {}: {}", tx.getTxHash(), e.getMessage());
                }
            }
        }
        if (!redeemers.isEmpty()) {
            builder.redeemers(redeemers);
        }

        List<PlutusData> plutusDataList = new ArrayList<>();
        if (witnesses.getDatums() != null) {
            for (var datum : witnesses.getDatums()) {
                try {
                    plutusDataList.add(PlutusData.deserialize(HexUtil.decodeHexString(datum.getCbor())));
                } catch (Exception e) {
                    log.debug("Skipping malformed datum for tx {}: {}", tx.getTxHash(), e.getMessage());
                }
            }
        }
        if (!plutusDataList.isEmpty()) {
            builder.plutusDataList(plutusDataList);
        }

        return builder.build();
    }

    /**
     * Builds the auxiliary data from metadata AND Alonzo+ auxiliary-data scripts (native/PlutusV1-3) so
     * scripts attached only via auxiliary data (not the witness set) aren't lost. Returns {@code null} when
     * there's no aux data at all, or nothing could be mapped from it (caller then emits CBOR {@code null}).
     */
    private static AuxiliaryData buildAuxiliaryData(com.bloxbean.cardano.yaci.helper.model.Transaction tx) {
        var auxData = tx.getAuxData();
        if (auxData == null) {
            return null;
        }

        AuxiliaryData.AuxiliaryDataBuilder builder = AuxiliaryData.builder();
        boolean hasContent = false;

        String metadataCborHex = auxData.getMetadataCbor();
        if (metadataCborHex != null && !metadataCborHex.isEmpty()) {
            try {
                DataItem metadataDataItem = CborSerializationUtil.deserializeOne(HexUtil.decodeHexString(metadataCborHex));
                CBORMetadata cborMetadata = CBORMetadata.deserialize((Map) metadataDataItem);
                builder.metadata(cborMetadata);
                hasContent = true;
            } catch (Exception e) {
                log.debug("Skipping malformed auxiliary metadata for tx {}: {}", tx.getTxHash(), e.getMessage());
            }
        }

        List<NativeScript> nativeScripts = new ArrayList<>();
        if (auxData.getNativeScripts() != null) {
            for (var nativeScript : auxData.getNativeScripts()) {
                try {
                    nativeScripts.add(NativeScript.deserializeJson(nativeScript.getContent()));
                } catch (Exception e) {
                    log.debug("Skipping malformed auxiliary native script for tx {}: {}", tx.getTxHash(), e.getMessage());
                }
            }
        }
        if (!nativeScripts.isEmpty()) {
            builder.nativeScripts(nativeScripts);
            hasContent = true;
        }

        List<PlutusV1Script> plutusV1Scripts = new ArrayList<>();
        if (auxData.getPlutusV1Scripts() != null) {
            for (PlutusScript plutusScript : auxData.getPlutusV1Scripts()) {
                try {
                    plutusV1Scripts.add(PlutusV1Script.deserialize(decodeAsByteString(plutusScript.getContent())));
                } catch (Exception e) {
                    log.debug("Skipping malformed auxiliary PlutusV1 script for tx {}: {}", tx.getTxHash(), e.getMessage());
                }
            }
        }
        if (!plutusV1Scripts.isEmpty()) {
            builder.plutusV1Scripts(plutusV1Scripts);
            hasContent = true;
        }

        List<PlutusV2Script> plutusV2Scripts = new ArrayList<>();
        if (auxData.getPlutusV2Scripts() != null) {
            for (PlutusScript plutusScript : auxData.getPlutusV2Scripts()) {
                try {
                    plutusV2Scripts.add(PlutusV2Script.deserialize(decodeAsByteString(plutusScript.getContent())));
                } catch (Exception e) {
                    log.debug("Skipping malformed auxiliary PlutusV2 script for tx {}: {}", tx.getTxHash(), e.getMessage());
                }
            }
        }
        if (!plutusV2Scripts.isEmpty()) {
            builder.plutusV2Scripts(plutusV2Scripts);
            hasContent = true;
        }

        List<PlutusV3Script> plutusV3Scripts = new ArrayList<>();
        if (auxData.getPlutusV3Scripts() != null) {
            for (PlutusScript plutusScript : auxData.getPlutusV3Scripts()) {
                try {
                    plutusV3Scripts.add(PlutusV3Script.deserialize(decodeAsByteString(plutusScript.getContent())));
                } catch (Exception e) {
                    log.debug("Skipping malformed auxiliary PlutusV3 script for tx {}: {}", tx.getTxHash(), e.getMessage());
                }
            }
        }
        if (!plutusV3Scripts.isEmpty()) {
            builder.plutusV3Scripts(plutusV3Scripts);
            hasContent = true;
        }

        return hasContent ? builder.build() : null;
    }

    /**
     * Plutus script CBOR (yaci-core {@code PlutusScript#getContent()}) decodes to a CBOR bytestring
     * whose inner bytes are the compiled script -- mirrors the proven pattern used for plutus script
     * hashing in the {@code stores:script} module ({@code ScriptUtil#getPlutusScriptHash}).
     */
    private static ByteString decodeAsByteString(String scriptContentHex) {
        byte[] raw = HexUtil.decodeHexString(scriptContentHex);
        DataItem dataItem = CborSerializationUtil.deserializeOne(raw);
        if (dataItem instanceof ByteString byteString) {
            return byteString;
        }
        return new ByteString(raw);
    }
}

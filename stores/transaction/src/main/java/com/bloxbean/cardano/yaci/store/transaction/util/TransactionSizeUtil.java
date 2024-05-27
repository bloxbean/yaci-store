package com.bloxbean.cardano.yaci.store.transaction.util;

import co.nstant.in.cbor.model.Array;
import co.nstant.in.cbor.model.ByteString;
import co.nstant.in.cbor.model.DataItem;
import co.nstant.in.cbor.model.Map;
import co.nstant.in.cbor.model.SimpleValue;
import co.nstant.in.cbor.model.UnsignedInteger;
import com.bloxbean.cardano.client.exception.CborDeserializationException;
import com.bloxbean.cardano.client.exception.CborSerializationException;
import com.bloxbean.cardano.client.transaction.spec.script.NativeScript;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.core.model.PlutusScript;
import com.bloxbean.cardano.yaci.core.util.CborSerializationUtil;
import com.bloxbean.cardano.yaci.helper.model.Transaction;
import com.bloxbean.cardano.yaci.store.transaction.domain.TransactionSize;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.misc.Pair;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class TransactionSizeUtil {

    /**
     * This functions reconstructs the original transaction and calculates the size and scriptSize from these.
     * @param tx Transaction to be parsed.
     * @return Pair containing TransactionSize as A and scriptSize as B
     */
    public static TransactionSize getTransactionSizeAndScriptSize(Transaction tx) {
        // Reconstructing Transaction based on CDDL specification - https://github.com/IntersectMBO/cardano-ledger/blob/master/eras/alonzo/impl/cddl-files/alonzo.cddl
        Map signedTransaction = new Map();
        if(tx.getBody().getCbor() == null) {
            // YACIConfig.INSTANCE.setReturnTxBodyCbor is false no cbor body contained
            return TransactionSize.builder()
                    .size(0)
                    .scriptSize(0)
                    .build();
        }
        DataItem txBody = CborSerializationUtil.deserialize(HexUtil.decodeHexString(tx.getBody().getCbor()))[0];
        signedTransaction.put(new UnsignedInteger(0), txBody);
        int scriptSize = addWitnessSetToSignedTransaction(tx, signedTransaction);

        if(tx.getBlockNumber() > 64902L) { // starting from alonzo era
            signedTransaction.put(new UnsignedInteger(
                    2), SimpleValue.TRUE);
        }

        addAuxDataToTransaction(tx, signedTransaction);

        byte[] serialize = CborSerializationUtil.serialize(signedTransaction);

        int txSize = serialize.length;
        return TransactionSize.builder()
                .size(txSize)
                .scriptSize(scriptSize)
                .build();
    }

    /**
     * Reconstructing the Witness_set from transaction and adding it to signedTransaction
     * CDDL Definition: https://github.com/IntersectMBO/cardano-ledger/blob/e6b6d4f85fb72b5cb5b5361e534d3bb71bb9e55e/eras/alonzo/impl/cddl-files/alonzo.cddl#L269
     * @param tx
     * @param signedTransaction
     * @return
     */
    private static int addWitnessSetToSignedTransaction(Transaction tx, Map signedTransaction) {
        // adding witnesses to signedTransaction
        Map witnessSet = new Map();
        int scriptSize = 0;
        addvKeyWitnessToWitness(tx, witnessSet);
        addNativeScriptsToWitness(tx, witnessSet);
        addBootstrapToWitness(tx, witnessSet);

        scriptSize += addPlutusToWitness(witnessSet, tx.getWitnesses().getPlutusV1Scripts(),
                3);
        scriptSize += addPlutusToWitness(witnessSet, tx.getWitnesses().getPlutusV2Scripts(), 6);
        scriptSize += addPlutusToWitness(witnessSet, tx.getWitnesses().getPlutusV3Scripts(), 7);

        addDatumToWitness(tx, witnessSet);
        addRedeemerToWitness(tx, witnessSet);

        if(!witnessSet.getKeys().isEmpty()) {
            signedTransaction.put(new UnsignedInteger(1), witnessSet);
        }
        return scriptSize;
    }

    /**
     * Adding the datum to Witness set. CDDL spec: https://github.com/IntersectMBO/cardano-ledger/blob/e6b6d4f85fb72b5cb5b5361e534d3bb71bb9e55e/eras/alonzo/impl/cddl-files/alonzo.cddl#L280
     * @param tx transaction to extract the datum
     * @param witnessSet witnessSet to add the datum
     */
    private static void addDatumToWitness(Transaction tx, Map witnessSet) {
        if(!tx.getWitnesses().getDatums().isEmpty()) {
            Array array = new Array();
            // could speed it up by passing an empty array, since we are only interested in the size not the content
            tx.getWitnesses().getDatums().forEach(datum -> array.add(new ByteString(HexUtil.decodeHexString(datum.getCbor()))));
            witnessSet.put(new UnsignedInteger(4), array);
        }
    }

    /**
     * Adding Plutus Script data to witnessSet. Can be used for V1, V2 and V3. CDDL spec: https://github.com/IntersectMBO/cardano-ledger/blob/e6b6d4f85fb72b5cb5b5361e534d3bb71bb9e55e/eras/alonzo/impl/cddl-files/alonzo.cddl#L278
     * @param witnessSet witnessset to add the data
     * @param scripts List of PlutusScripts
     * @param witnessSetIndex Index where to add the datum based on cddl spec
     * @return
     */
    private static int addPlutusToWitness(Map witnessSet, List<PlutusScript> scripts, int witnessSetIndex) {
        AtomicInteger scriptSize = new AtomicInteger();
        Array array = new Array();
        if(!scripts.isEmpty()) {
            scripts.forEach(script -> {
                scriptSize.addAndGet(script.getContent().length() / 2); // adding have the string length, sinze it's 4bit hex and we need the byte length
                array.add(new ByteString(HexUtil.decodeHexString(script.getContent())));
            });
            witnessSet.put(new UnsignedInteger(witnessSetIndex), array);
        }
        return scriptSize.get();
    }

    /**
     * Adding Redemer data to witnessset. CDDL spec: https://github.com/IntersectMBO/cardano-ledger/blob/e6b6d4f85fb72b5cb5b5361e534d3bb71bb9e55e/eras/alonzo/impl/cddl-files/alonzo.cddl#L302
     * @param tx transaction to extract the redeemer data
     * @param witnessSet witnesset to add the data based on cddl spec
     */
    private static void addRedeemerToWitness(Transaction tx, Map witnessSet) {
        if(!tx.getWitnesses().getRedeemers().isEmpty()) {
            Array array = new Array();
            // could speed it up by passing an empty array, since we are only interested in the size not the content
            tx.getWitnesses().getRedeemers().forEach(redeemer -> array.add(new ByteString(HexUtil.decodeHexString(redeemer.getCbor()))));
            witnessSet.put(new UnsignedInteger(5), array);
        }
    }

    /**
     * Extracting bootstrap data and adding it to witnessSet. CDDL spec: https://github.com/IntersectMBO/cardano-ledger/blob/e6b6d4f85fb72b5cb5b5361e534d3bb71bb9e55e/eras/alonzo/impl/cddl-files/alonzo.cddl#L348
     * @param tx Transcation to get the bootstrap from
     * @param witnessSet witnessSet to add the data
     */
    private static void addBootstrapToWitness(Transaction tx, Map witnessSet) {
        if(!tx.getWitnesses().getBootstrapWitnesses().isEmpty()) {
            Array array = new Array();
            tx.getWitnesses().getBootstrapWitnesses().forEach(bootstrapWitness -> {
                Array witnessArray = new Array();
                witnessArray.add(new ByteString(HexUtil.decodeHexString(bootstrapWitness.getPublicKey())));
                witnessArray.add(new ByteString(HexUtil.decodeHexString(bootstrapWitness.getSignature())));
                witnessArray.add(new ByteString(HexUtil.decodeHexString(bootstrapWitness.getChainCode())));
                witnessArray.add(new ByteString(HexUtil.decodeHexString(bootstrapWitness.getAttributes())));
                array.add(witnessArray);
            });
            witnessSet.put(new UnsignedInteger(2), array);
        }
    }

    /**
     * Extracting the Auxiliary data and adding it to the transaction. CDDL spec: https://github.com/IntersectMBO/cardano-ledger/blob/e6b6d4f85fb72b5cb5b5361e534d3bb71bb9e55e/eras/alonzo/impl/cddl-files/alonzo.cddl#L17
     * @param tx Transcation to get the auxdata from
     * @param signedTransaction Map to add the auxdata
     */
    private static void addAuxDataToTransaction(Transaction tx, Map signedTransaction) {
        if(tx.getAuxData() != null)  {
            Array auxiliaryData = new Array();
            if(tx.getAuxData().getMetadataCbor() != null) {
                auxiliaryData.add(CborSerializationUtil.deserialize(
                        HexUtil.decodeHexString(tx.getAuxData().getMetadataCbor()))[0]);
            }
            signedTransaction.put(new UnsignedInteger(3), auxiliaryData);
        }
    }

    /**
     * Extracting VKey and adding it to witnessSet. CDDL spec: https://github.com/IntersectMBO/cardano-ledger/blob/e6b6d4f85fb72b5cb5b5361e534d3bb71bb9e55e/eras/alonzo/impl/cddl-files/alonzo.cddl#L346
     * @param tx transaction to extract the data
     * @param witnessSet witnessSet to add the data
     */
    private static void addvKeyWitnessToWitness(Transaction tx, Map witnessSet) {
        if(!tx.getWitnesses().getVkeyWitnesses().isEmpty()) {
            Array vKeyWitnessArray = new Array();
            tx.getWitnesses().getVkeyWitnesses().forEach(vkeyWitness -> {
                Array vitnessArray = new Array();
                vitnessArray.add(new ByteString(HexUtil.decodeHexString(vkeyWitness.getKey())));
                vitnessArray.add(new ByteString(HexUtil.decodeHexString(vkeyWitness.getSignature()))); // could speed it up by passing an empty array, since we are only interested in the size not the content
                vKeyWitnessArray.add(vitnessArray);
            });
            witnessSet.put(new UnsignedInteger(0), vKeyWitnessArray);
        }
    }

    /**
     * Extracting NativeScript data and adding it to witnessSet. CDDL spec: https://github.com/IntersectMBO/cardano-ledger/blob/e6b6d4f85fb72b5cb5b5361e534d3bb71bb9e55e/eras/alonzo/impl/cddl-files/alonzo.cddl#L355
     * @param tx transaction to extract the data
     * @param witnessSet witnessSet to add the data
     */
    private static void addNativeScriptsToWitness(Transaction tx, Map witnessSet) {
        if(!tx.getWitnesses().getNativeScripts().isEmpty()) {
            Array nativeScripts = new Array();
            tx.getWitnesses().getNativeScripts().forEach(script -> {
                NativeScript nativeScript;
                try {
                    nativeScript = NativeScript.deserializeJson(script.getContent());
                    nativeScripts.add(new ByteString(nativeScript.getScriptHash()));
                } catch (CborDeserializationException | JsonProcessingException |
                         CborSerializationException e) {
                    log.error("Can't parse Native script for Transaction: " + tx.getTxHash());
                }
            });
            witnessSet.put(new UnsignedInteger(1), nativeScripts);
        }
    }

}

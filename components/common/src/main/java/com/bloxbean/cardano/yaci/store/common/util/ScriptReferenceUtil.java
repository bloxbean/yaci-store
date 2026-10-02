package com.bloxbean.cardano.yaci.store.common.util;

import co.nstant.in.cbor.model.Array;
import co.nstant.in.cbor.model.ByteString;
import co.nstant.in.cbor.model.DataItem;
import co.nstant.in.cbor.model.UnsignedInteger;
import com.bloxbean.cardano.client.exception.CborRuntimeException;
import com.bloxbean.cardano.client.exception.CborSerializationException;
import com.bloxbean.cardano.client.plutus.spec.PlutusV1Script;
import com.bloxbean.cardano.client.plutus.spec.PlutusV2Script;
import com.bloxbean.cardano.client.plutus.spec.PlutusV3Script;
import com.bloxbean.cardano.client.spec.Script;
import com.bloxbean.cardano.client.transaction.spec.script.NativeScript;
import com.bloxbean.cardano.yaci.core.util.HexUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class ScriptReferenceUtil {

    //TODO -- Move this to cardano-client-lib
    /**
     * Deserialize to {@link Script} based on the type in serialized bytes.
     * Serialized bytes is a Cbor Array follows cardano-cli format. This is a fallback method if the standard deserialization is
     * not successful.
     * The serializedPlutusScript parameter contains both type and script body.
     * <p>
     * A deeply nested native script is reported as {@link CborRuntimeException} instead of {@link StackOverflowError}.
     * Callers on the sync path must catch it, otherwise the sync stops.
     * TODO: Revisit once CCL provides stack-safe decoding (cardano-client-lib#681)
     *
     * @param serializedScriptRef
     * @return PlutusV1Script or PlutusV2Script or PlutusV3Script
     * @throws CborRuntimeException if the script ref is invalid or too deeply nested
     */
    public static Script deserializeScriptRef(byte[] serializedScriptRef) {
        try {
            return deserialize(serializedScriptRef);
        } catch (StackOverflowError e) {
            //Deeply nested native script, CCL cbor decoding is recursive
            throw new CborRuntimeException("Script deserialization failed. Script ref is too deeply nested");
        }
    }

    private static Script deserialize(byte[] serializedScriptRef) {
        Array scriptArray = (Array) com.bloxbean.cardano.client.common.cbor.CborSerializationUtil.deserialize(serializedScriptRef);
        List<DataItem> dataItemList = scriptArray.getDataItems();
        if (dataItemList == null || dataItemList.size() == 0) {
            throw new CborRuntimeException("Script deserialization failed. Invalid no of DataItem");
        }

        int type = ((UnsignedInteger) dataItemList.get(0)).getValue().intValue();
        try {
            if (type == 0) { //Native script
                Array scriptBytes = ((Array) dataItemList.get(1));
                return NativeScript.deserialize(scriptBytes);
            } else if (type == 1) {
                ByteString scriptBytes = ((ByteString) dataItemList.get(1));
                return PlutusV1Script.deserialize(scriptBytes);
            } else if (type == 2) {
                ByteString scriptBytes = ((ByteString) dataItemList.get(1));
                return PlutusV2Script.deserialize(scriptBytes);
            } else if (type == 3) {
                ByteString scriptBytes = ((ByteString) dataItemList.get(1));
                return PlutusV3Script.deserialize(scriptBytes);
            } else {
                throw new CborRuntimeException("Invalid type : " + type);
            }
        } catch (Exception e) {
            throw new CborRuntimeException("Script deserialization failed.", e);
        }
    }

    /**
     * Get the script hash from the script reference bytes
     * <p>
     * A deeply nested native script is reported as {@link CborRuntimeException} instead of {@link StackOverflowError}.
     * Callers on the sync path must catch it, otherwise the sync stops.
     * TODO: Revisit once CCL provides stack-safe decoding (cardano-client-lib#681)
     *
     * @param scriptRefBytes
     * @return script hash
     * @throws CborRuntimeException if the script ref is invalid or too deeply nested
     */
    public static String getReferenceScriptHash(byte[] scriptRefBytes) throws CborSerializationException {
        Script script = deserializeScriptRef(scriptRefBytes);
        try {
            return HexUtil.encodeHexString(script.getScriptHash());
        } catch (StackOverflowError e) {
            //Deeply nested native script, CCL cbor serialization is recursive
            throw new CborRuntimeException("Unable to calculate script hash. Script ref is too deeply nested");
        }
    }
}

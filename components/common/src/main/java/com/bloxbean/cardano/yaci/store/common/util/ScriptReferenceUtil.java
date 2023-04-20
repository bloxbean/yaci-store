package com.bloxbean.cardano.yaci.store.common.util;

import co.nstant.in.cbor.model.Array;
import co.nstant.in.cbor.model.ByteString;
import co.nstant.in.cbor.model.DataItem;
import co.nstant.in.cbor.model.UnsignedInteger;
import com.bloxbean.cardano.client.exception.CborRuntimeException;
import com.bloxbean.cardano.client.exception.CborSerializationException;
import com.bloxbean.cardano.client.transaction.spec.PlutusV1Script;
import com.bloxbean.cardano.client.transaction.spec.PlutusV2Script;
import com.bloxbean.cardano.client.transaction.spec.script.NativeScript;
import com.bloxbean.cardano.client.transaction.spec.script.Script;
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
     * @param serializedScriptRef
     * @return PlutusV1Script or PlutusV2Script
     */
    private static Script deserializeScriptRef(byte[] serializedScriptRef) {
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
            } else
                if (type == 1) {
                ByteString scriptBytes = ((ByteString) dataItemList.get(1));
                return PlutusV1Script.deserialize(scriptBytes);
            } else if (type == 2) {
                ByteString scriptBytes = ((ByteString) dataItemList.get(1));
                return PlutusV2Script.deserialize(scriptBytes);
            } else {
                throw new CborRuntimeException("Invalid type : " + type);
            }
        } catch (Exception e) {
            throw new CborRuntimeException("Script deserialization failed.", e);
        }
    }

    /**
     * Get the script hash from the script reference bytes
     * @param scriptRefBytes
     * @return script hash
     */
    public static String getReferenceScriptHash(byte[] scriptRefBytes) throws CborSerializationException {
        Script script = deserializeScriptRef(scriptRefBytes);
        return HexUtil.encodeHexString(script.getScriptHash());
    }
}

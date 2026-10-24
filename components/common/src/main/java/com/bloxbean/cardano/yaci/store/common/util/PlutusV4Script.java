package com.bloxbean.cardano.yaci.store.common.util;

import co.nstant.in.cbor.CborException;
import co.nstant.in.cbor.model.Array;
import co.nstant.in.cbor.model.ByteString;
import co.nstant.in.cbor.model.UnsignedInteger;
import com.bloxbean.cardano.client.common.cbor.CborSerializationUtil;
import com.bloxbean.cardano.client.exception.CborSerializationException;
import com.bloxbean.cardano.client.spec.Script;
import com.bloxbean.cardano.yaci.core.util.HexUtil;
import lombok.EqualsAndHashCode;
import lombok.Getter;

//TODO -- Replace with cardano-client-lib's Plutus V4 script once available (cardano-client-lib#707)
/**
 * Plutus V4 script (Dijkstra era, script type 4). Only what Yaci Store needs from a script reference: the script
 * hash, blake2b-224 of {@code 0x04 || script bytes}, computed like cardano-client-lib does for Plutus V1-V3.
 * Building or evaluating Plutus V4 transactions is not supported.
 */
@Getter
@EqualsAndHashCode
public class PlutusV4Script implements Script {
    public static final int TYPE = 4;
    public static final String TYPE_NAME = "PlutusScriptV4";

    private final String type = TYPE_NAME;
    //CBOR byte string of the script, as for PlutusV1Script, PlutusV2Script and PlutusV3Script
    private final String cborHex;

    private PlutusV4Script(String cborHex) {
        this.cborHex = cborHex;
    }

    public static PlutusV4Script deserialize(ByteString scriptBytes) throws CborException {
        return new PlutusV4Script(HexUtil.encodeHexString(CborSerializationUtil.serialize(scriptBytes)));
    }

    @Override
    public ByteString serializeAsDataItem() {
        return (ByteString) CborSerializationUtil.deserialize(HexUtil.decodeHexString(cborHex));
    }

    @Override
    public byte[] serializeScriptBody() {
        return serializeAsDataItem().getBytes();
    }

    @Override
    public byte[] scriptRefBytes() throws CborSerializationException {
        Array array = new Array();
        array.add(new UnsignedInteger(TYPE));
        array.add(serializeAsDataItem());
        try {
            return CborSerializationUtil.serialize(array);
        } catch (CborException e) {
            throw new CborSerializationException("Cbor serialization error", e);
        }
    }

    @Override
    public byte[] getScriptTypeBytes() {
        return new byte[]{(byte) TYPE};
    }

    @Override
    public int getScriptType() {
        return TYPE;
    }
}

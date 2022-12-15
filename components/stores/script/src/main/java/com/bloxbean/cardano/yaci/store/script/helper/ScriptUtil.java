package com.bloxbean.cardano.yaci.store.script.helper;

import co.nstant.in.cbor.model.Array;
import co.nstant.in.cbor.model.ByteString;
import co.nstant.in.cbor.model.DataItem;
import co.nstant.in.cbor.model.UnsignedInteger;
import com.bloxbean.cardano.client.exception.CborDeserializationException;
import com.bloxbean.cardano.client.exception.CborRuntimeException;
import com.bloxbean.cardano.client.transaction.spec.PlutusData;
import com.bloxbean.cardano.client.transaction.spec.PlutusV1Script;
import com.bloxbean.cardano.client.transaction.spec.PlutusV2Script;
import com.bloxbean.cardano.client.transaction.spec.Redeemer;
import com.bloxbean.cardano.yaci.core.model.Datum;
import com.bloxbean.cardano.yaci.core.model.NativeScript;
import com.bloxbean.cardano.yaci.core.model.PlutusScript;
import com.bloxbean.cardano.yaci.core.util.CborSerializationUtil;
import com.bloxbean.cardano.yaci.core.util.HexUtil;
import com.bloxbean.cardano.yaci.store.script.model.ScriptType;
import com.bloxbean.cardano.yaci.store.utxo.model.AddressUtxoEntity;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Slf4j
public class ScriptUtil {

    public static String getNativeScriptHash(NativeScript nativeScript) {
        try {
            com.bloxbean.cardano.client.transaction.spec.script.NativeScript nativeScript1
                    = com.bloxbean.cardano.client.transaction.spec.script.NativeScript.deserializeJson(nativeScript.getContent());
            return HexUtil.encodeHexString(nativeScript1.getScriptHash());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static String getPlutusScriptHash(PlutusScript plutusScript) {
        byte[] content = HexUtil.decodeHexString(plutusScript.getContent());

        try {
            if ("1".equals(plutusScript.getType())) {
                PlutusV1Script plutusV1Script =
                        PlutusV1Script.deserialize((ByteString) CborSerializationUtil.deserializeOne(content));
                return HexUtil.encodeHexString(plutusV1Script.getScriptHash());
            } else if ("2".equals(plutusScript.getType())) {
                PlutusV2Script plutusV2Script =
                        PlutusV2Script.deserialize((ByteString) CborSerializationUtil.deserializeOne(content));
                return HexUtil.encodeHexString(plutusV2Script.getScriptHash());
            } else {
                throw new IllegalArgumentException("Invalid plutus script type : " + plutusScript);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static ScriptType toPlutusScriptType(@NotNull String strType) {
        if ("1".equals(strType))
            return ScriptType.PLUTUS_V1;
        else if ("2".equals(strType))
            return ScriptType.PLUTUS_V2;
        else
            throw new IllegalArgumentException("Invalid plutus script type : " + strType);
    }

    /**
     * Deserialize to {@link PlutusV1Script} or {@link PlutusV2Script} based on the type in serialized bytes.
     * Serialized bytes is a Cbor Array follows cardano-cli format. This is a fallback method if the standard deserialization is
     * not successful.
     * The serializedPlutusScript parameter contains both type and script body.
     * @param serializedPlutusScript
     * @return PlutusV1Script or PlutusV2Script
     */
    public static com.bloxbean.cardano.client.transaction.spec.PlutusScript deserializeScriptRef(byte[] serializedPlutusScript) {
        Array plutusScriptArray = (Array) com.bloxbean.cardano.client.common.cbor.CborSerializationUtil.deserialize(serializedPlutusScript);
        List<DataItem> dataItemList = plutusScriptArray.getDataItems();
        if (dataItemList == null || dataItemList.size() == 0) {
            throw new CborRuntimeException("PlutusScript deserialization failed. Invalid no of DataItem");
        }

        int type = ((UnsignedInteger) dataItemList.get(0)).getValue().intValue();
        ByteString scriptBytes = ((ByteString) dataItemList.get(1));
        try {
            if (type == 1) {
                return PlutusV1Script.deserialize(scriptBytes);
            } else if (type == 2) {
                return PlutusV2Script.deserialize(scriptBytes);
            } else {
                throw new CborRuntimeException("Invalid type : " + type);
            }
        } catch (Exception e) {
            throw new CborRuntimeException("PlutusScript deserialization failed.", e);
        }
    }

    public static PlutusScript deserializeScriptRef(@NonNull AddressUtxoEntity addressUtxo) {
        try {
            com.bloxbean.cardano.client.transaction.spec.PlutusScript cclPlutusScript
                    = ScriptUtil.deserializeScriptRef(HexUtil.decodeHexString(addressUtxo.getScriptRef()));
            PlutusScript plutusScript = new PlutusScript(String.valueOf(cclPlutusScript.getScriptType()), cclPlutusScript.getCborHex());
            return plutusScript;
        } catch (Exception e) {
            log.error("Error deserializing plutus script in scriptRef : TxHash: " + addressUtxo.getTxHash()
                    + "#" + addressUtxo.getOutputIndex() + ", scriptRef: " + addressUtxo.getScriptRef());
            return null;
        }
    }

    public static String getDatumHash(Datum datum) {
        if (datum == null) return null;

        return getDatumHash(datum.getCbor());
    }

    public static String getDatumHash(String datumCbor) {
        if (datumCbor == null)
            return null;

        try {
            PlutusData plutusData = PlutusData.deserialize(HexUtil.decodeHexString(datumCbor));
            return plutusData.getDatumHash();
        } catch (Exception e) {
            log.error("Unable to deserialize and calculate datumhash for : " + datumCbor, e);
            return null;
        }
    }

    public static Optional<Redeemer> deserializeRedeemer(String redeemerCbor) {
        if (redeemerCbor == null)
            return Optional.empty();

        try {
            Redeemer redeemer = Redeemer.deserialize((Array) CborSerializationUtil.deserializeOne(HexUtil.decodeHexString(redeemerCbor)));
            return Optional.of(redeemer);
        } catch (CborDeserializationException e) {
            log.error("Error deserializing redeemerCbor");
            return Optional.empty();
        }
    }

    public static String serializePlutusData(PlutusData plutusData) {
        if (plutusData == null)
            return null;

        return plutusData.serializeToHex();
    }
}

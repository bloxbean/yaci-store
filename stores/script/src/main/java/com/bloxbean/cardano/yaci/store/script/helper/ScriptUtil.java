package com.bloxbean.cardano.yaci.store.script.helper;

import co.nstant.in.cbor.CborException;
import co.nstant.in.cbor.model.Array;
import co.nstant.in.cbor.model.ByteString;
import co.nstant.in.cbor.model.DataItem;
import co.nstant.in.cbor.model.UnsignedInteger;
import com.bloxbean.cardano.client.exception.CborDeserializationException;
import com.bloxbean.cardano.client.exception.CborRuntimeException;
import com.bloxbean.cardano.client.plutus.spec.PlutusData;
import com.bloxbean.cardano.client.plutus.spec.PlutusV1Script;
import com.bloxbean.cardano.client.plutus.spec.PlutusV2Script;
import com.bloxbean.cardano.client.plutus.spec.Redeemer;
import com.bloxbean.cardano.client.util.JsonUtil;
import com.bloxbean.cardano.yaci.core.model.Datum;
import com.bloxbean.cardano.yaci.core.model.NativeScript;
import com.bloxbean.cardano.yaci.core.model.PlutusScript;
import com.bloxbean.cardano.yaci.core.model.PlutusScriptType;
import com.bloxbean.cardano.yaci.core.util.CborSerializationUtil;
import com.bloxbean.cardano.yaci.core.util.HexUtil;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.util.PlutusV3Script;
import com.bloxbean.cardano.yaci.store.script.domain.Script;
import com.bloxbean.cardano.yaci.store.script.domain.ScriptType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
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
            if (PlutusScriptType.PlutusScriptV1 == plutusScript.getType()) {
                PlutusV1Script plutusV1Script =
                        PlutusV1Script.deserialize((ByteString) CborSerializationUtil.deserializeOne(content));
                return HexUtil.encodeHexString(plutusV1Script.getScriptHash());
            } else if (PlutusScriptType.PlutusScriptV2 == plutusScript.getType()) {
                PlutusV2Script plutusV2Script =
                        PlutusV2Script.deserialize((ByteString) CborSerializationUtil.deserializeOne(content));
                return HexUtil.encodeHexString(plutusV2Script.getScriptHash());
            } else if (PlutusScriptType.PlutusScriptV3 == plutusScript.getType()) {
                PlutusV3Script plutusV3Script =
                        PlutusV3Script.deserialize((ByteString) CborSerializationUtil.deserializeOne(content));
                return HexUtil.encodeHexString(plutusV3Script.getScriptHash());
            } else {
                throw new IllegalArgumentException("Invalid plutus script type : " + plutusScript.getType());
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static ScriptType toPlutusScriptType(@NotNull PlutusScriptType strType) {
        if (PlutusScriptType.PlutusScriptV1 == strType)
            return ScriptType.PLUTUS_V1;
        else if (PlutusScriptType.PlutusScriptV2 == strType)
            return ScriptType.PLUTUS_V2;
        else if (PlutusScriptType.PlutusScriptV3 == strType)
            return ScriptType.PLUTUS_V3;
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
    public static com.bloxbean.cardano.client.plutus.spec.PlutusScript deserializeScriptRef(byte[] serializedPlutusScript) {
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
            } else if (type == 3) {
                return PlutusV3Script.deserialize(scriptBytes);
            } else {
                throw new CborRuntimeException("Invalid type : " + type);
            }
        } catch (Exception e) {
            throw new CborRuntimeException("PlutusScript deserialization failed.", e);
        }
    }

    public static PlutusScript deserializeScriptRef(@NonNull AddressUtxo addressUtxo) {
        var plutusScript = deserializeScriptRef(addressUtxo.getScriptRef());
        if (plutusScript == null) {
            log.error("Error deserializing plutus script in scriptRef : TxHash: " + addressUtxo.getTxHash()
                    + "#" + addressUtxo.getOutputIndex() + ", scriptRef: " + addressUtxo.getScriptRef());
        }

        return plutusScript;
    }

    public static PlutusScript deserializeScriptRef(String scriptRef) {
        try {
            com.bloxbean.cardano.client.plutus.spec.PlutusScript cclPlutusScript
                    = ScriptUtil.deserializeScriptRef(HexUtil.decodeHexString(scriptRef));

            var plutusScriptType = switch (cclPlutusScript.getScriptType()) {
                case 1 -> PlutusScriptType.PlutusScriptV1;
                case 2 -> PlutusScriptType.PlutusScriptV2;
                case 3 -> PlutusScriptType.PlutusScriptV3;
                default -> throw new IllegalArgumentException("Invalid plutus script type : " + cclPlutusScript.getScriptType());
            };

            PlutusScript plutusScript = new PlutusScript(plutusScriptType, cclPlutusScript.getCborHex());
            return plutusScript;
        } catch (Exception e) {
            log.error("Error deserializing script ref: {}", scriptRef);
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

    public static byte[] removeDoubleEncoding(Script script) throws JsonProcessingException, CborException {
        if (script.getContent() == null && script.getScriptType() == ScriptType.NATIVE_SCRIPT)
            return null;

        JsonNode contentNode = JsonUtil.parseJson(script.getContent());
        String content = contentNode.get("content").asText();
        byte[] bytes = com.bloxbean.cardano.client.util.HexUtil.decodeHexString(content);
        DataItem dataItem = com.bloxbean.cardano.client.common.cbor.CborSerializationUtil.deserialize(bytes);
        if (dataItem instanceof ByteString) { //double encoding
            dataItem = com.bloxbean.cardano.client.common.cbor.CborSerializationUtil.deserialize(((ByteString) dataItem).getBytes());
            return com.bloxbean.cardano.client.common.cbor.CborSerializationUtil.serialize(dataItem);
        }
        return bytes;
    }
}

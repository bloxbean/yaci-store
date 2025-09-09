package com.bloxbean.cardano.yaci.store.cip139.utxo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class UtxoDto {
    private List<TransactionUnspentOutput> utxos;

    private record TransactionUnspentOutput(List<TxInput> input, List<TxOutput> output){}

    private record TxInput(String transactionId, Integer index){}

    private record TxOutput(String address, TxAmount amount, PlutusData plutusData, ScriptRef scriptRef){}

    private record TxAmount(BigInteger coin, Map<String, Map<String, BigInteger>> assets){}

    private record PlutusData(String tag, Datum datum, DatumHash datumHash){}

    private record Datum(String tag, DatumValue value){}

    private record DatumValue(String tag, String value, String alternative, List<String> contents, List<MappedEntry> mappedContents){}

    private record MappedEntry(String key, String value){}

    private record DatumHash(String tag, String value){}

    private record ScriptRef(String tag, PlutusScript plutusScript, NativeScript nativeScript){}

    private record PlutusScript(String tag, PlutusScriptValue value){}

    private record PlutusScriptValue(String language, String bytes){}

    private record NativeScript(String tag, NativeScriptValue value){}

    private record NativeScriptValue(String tag, String pubkey, List<String> scripts, Integer n, String slot){}


}


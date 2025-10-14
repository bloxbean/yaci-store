package com.bloxbean.cardano.yaci.store.cip139.transaction.dto;

import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.store.common.util.JsonUtil;
import com.bloxbean.cardano.yaci.store.common.util.ScriptReferenceUtil;
import com.bloxbean.cardano.yaci.store.transaction.domain.Txn;
import com.bloxbean.cardano.yaci.store.transaction.domain.TxnWitness;
import com.bloxbean.cardano.yaci.store.transaction.processor.TransactionProcessor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.util.*;

import static com.bloxbean.cardano.yaci.core.util.Constants.LOVELACE;

@Slf4j
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TransactionDto {

    private AuxiliaryData auxiliaryData;
    private TransactionBody body;
    private boolean isValid;
    private WitnessSet witnessSet;

    record TxInput(String transactionId, Integer index){}
    //record TxOutput(String address, TxAmount amount, PlutusData plutusData, ScriptRef scriptRef){}
    record TxOutput(String transactionId, Integer index){}
    record TxAmount(BigInteger coin, Map<String, Map<String, BigInteger>> assets){}
    record ScriptRef(String tag, @JsonProperty("value") PlutusScript plutusScript, @JsonProperty("value") NativeScript nativeScript){}


    //record AuxiliaryData(List<Metadata> metadata, List<NativeScript> nativeScripts, List<PlutusScript> plutusScripts){}
    record AuxiliaryData(String message){}

    //record WitnessSet(List<Bootstrap> bootstraps, List<NativeScript> nativeScripts, List<PlutusData> plutusData, List<PlutusScript> plutusScripts, List<Redeemer> redeemers, List<VKeyWitness> vkeywitnesses){}
    record WitnessSet(String message){}

    record PlutusScript(String language, String bytes){}
    record NativeScript(String tag, String value){}
    record PlutusData(String tag, String value){}


    public static TransactionDto fromDomain(Txn txn, List<TxnWitness> txnWitness){
        TransactionDto transactionDto = new TransactionDto();

        AuxiliaryData auxiliaryData = new AuxiliaryData("Not yet implemented");
        transactionDto.setAuxiliaryData(auxiliaryData);

        TransactionBody transactionBody = fromDomain(txn);
        transactionDto.setBody(transactionBody);

        transactionDto.setValid(!txn.getInvalid());

        WitnessSet witnessSet = new WitnessSet("Not yet implemented");
        transactionDto.setWitnessSet(witnessSet);

        return transactionDto;
    }

    private static TransactionBody fromDomain(Txn txn){

        return TransactionBody
                .builder()
                .auxiliaryDataHash(txn.getAuxiliaryDataHash())
                .inputs(txn.getInputs().stream().map(txnInput -> new TxInput(txnInput.getTxHash(), txnInput.getOutputIndex())).toList())
                .outputs(txn.getOutputs().stream().map(txnOutput -> new TxOutput(txnOutput.getTxHash(), txnOutput.getOutputIndex())).toList())
                .fee(String.valueOf(txn.getFee()))
                .certs("Not yet implemented")
                .collateral(txn.getCollateralInputs().stream().map(txnInput -> new TxInput(txnInput.getTxHash(), txnInput.getOutputIndex())).toList())
                .collateralReturn(new TxOutput(txn.getCollateralReturn().getTxHash(), txn.getCollateralReturn().getOutputIndex()))
                .mint("Not yet implemented")
                .networkId(String.valueOf(txn.getNetowrkId()))
                .referenceInputs(txn.getReferenceInputs().stream().map(txnInput -> new TxInput(txnInput.getTxHash(), txnInput.getOutputIndex())).toList())
                .requiredSigners(new ArrayList<>(txn.getRequiredSigners()))
                .scriptDataHash(txn.getScriptDataHash())
                .totalCollateral(String.valueOf(txn.getTotalCollateral()))
                .ttl(String.valueOf(txn.getTtl()))
                .update("Not yet implemented")
                .validityStartInterval(String.valueOf(txn.getValidityIntervalStart()))
                .withdrawals("Not yet implemented")
                .votingProcedures("Not yet implemented")
                .proposalProcedures("Not yet implemented")
                .build();
    }

}



@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
class TransactionBody{
    String auxiliaryDataHash;
    List<TransactionDto.TxInput> inputs;
    List<TransactionDto.TxOutput> outputs;
    String fee;
    String certs;
    List<TransactionDto.TxInput> collateral;
    TransactionDto.TxOutput collateralReturn;
    String mint;
    String networkId;
    List<TransactionDto.TxInput> referenceInputs;
    List<String> requiredSigners;
    String scriptDataHash;
    String totalCollateral;
    String ttl;
    String update;
    String validityStartInterval;
    String withdrawals;
    String votingProcedures;
    String proposalProcedures;
    String donation;
    String currentTreasuryValue;
}

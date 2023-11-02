package com.bloxbean.cardano.yaci.store.common.genesis;

import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.Data;

import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

import static com.bloxbean.cardano.yaci.store.common.genesis.util.PlutusKeys.PLUTUS_V1;

@Data
public class AlonzoGenesis extends GenesisFile {
    private static final String LOVELACE_PER_UTXO_WORD = "lovelacePerUTxOWord";
    private static final String EXECUTION_PRICES = "executionPrices";
    private static final String PR_STEPS = "prSteps";
    private static final String PR_MEM = "prMem";
    private static final String NUMERATOR = "numerator";
    private static final String DENOMINATOR = "denominator";
    private static final String MAX_TX_EX_UNITS = "maxTxExUnits";
    private static final String EX_UNITS_MEM = "exUnitsMem";
    private static final String EX_UNITS_STEPS = "exUnitsSteps";
    private static final String MAX_BLOCK_EX_UNITS = "maxBlockExUnits";
    private static final String MAX_VALUE_SIZE = "maxValueSize";
    private static final String COLLATERAL_PERCENTAGE = "collateralPercentage";
    private static final String MAX_COLLATERAL_INPUTS = "maxCollateralInputs";
    private static final String COST_MODELS = "costModels";

    private ObjectMapper objectMapper;

    public AlonzoGenesis() {

    }

    public AlonzoGenesis(File shelleyGenesisFile) {
        super(shelleyGenesisFile);
    }

    public AlonzoGenesis(InputStream in) {
        super(in);
    }

    public AlonzoGenesis(long protocolMagic) {
        super(protocolMagic);
    }

    @Override
    protected void readGenesisData(JsonNode genesisJson) {
        this.objectMapper = new ObjectMapper();
        BigInteger lovelacePerUTxOWord = genesisJson.get(LOVELACE_PER_UTXO_WORD).bigIntegerValue();

        var executionPriceNode = genesisJson.get(EXECUTION_PRICES);
        var prStepsNode = executionPriceNode.get(PR_STEPS);
        var prMemNode = executionPriceNode.get(PR_MEM);

        BigDecimal priceSteps;
        if (prStepsNode.isObject()) {
            var prStepsNumerator = prStepsNode.get(NUMERATOR).decimalValue();
            var prStepsDenominator = prStepsNode.get(DENOMINATOR).decimalValue();
            priceSteps = prStepsNumerator.divide(prStepsDenominator);
        } else {
            priceSteps = prStepsNode.decimalValue();
        }

        BigDecimal priceMem;
        if (prMemNode.isObject()) {
            var prMemNumerator = prMemNode.get(NUMERATOR).decimalValue();
            var prMemDenominator = prMemNode.get(DENOMINATOR).decimalValue();
            priceMem = prMemNumerator.divide(prMemDenominator);
        } else {
            priceMem = prMemNode.decimalValue();
        }

        var maxTxExUnitsNode = genesisJson.get(MAX_TX_EX_UNITS);
        var txExUnitsMem = maxTxExUnitsNode.get(EX_UNITS_MEM).bigIntegerValue();
        var txExUnitsSteps = maxTxExUnitsNode.get(EX_UNITS_STEPS).bigIntegerValue();

        var maxBlockExUnitsNode = genesisJson.get(MAX_BLOCK_EX_UNITS);
        var blockExUnitsMem = maxBlockExUnitsNode.get(EX_UNITS_MEM).bigIntegerValue();
        var blockExUnitsStep = maxBlockExUnitsNode.get(EX_UNITS_STEPS).bigIntegerValue();

        var maxValueSize = genesisJson.get(MAX_VALUE_SIZE).asLong();
        var collateralPercentage = genesisJson.get(COLLATERAL_PERCENTAGE).asInt();
        var maxCollateralInputs = genesisJson.get(MAX_COLLATERAL_INPUTS).asInt();

        var costModelNode = genesisJson.get(COST_MODELS);
        var plutusV1CostModelNode = costModelNode.get(PLUTUS_V1);

        long[] plutusV1Costs;
        if (plutusV1CostModelNode.isObject()) { //op -> cost
            Map<String, Long> plutusV1CostModelMap = objectMapper.convertValue(plutusV1CostModelNode, new TypeReference<SortedMap<String, Long>>() {
            });
            plutusV1Costs = plutusV1CostModelMap.values().stream().mapToLong(Long::longValue).toArray();
        } else if (plutusV1CostModelNode.isArray()) { //long[] costmodel
            var arrNode = ((ArrayNode)plutusV1CostModelNode);
            plutusV1Costs = new long[arrNode.size()];
            for (int i=0; i<arrNode.size(); i++) {
                plutusV1Costs[i] = arrNode.get(i).asLong();
            }
        } else
            throw new IllegalStateException("CostModel format in alonzo-genesis file isn not supported");

        Map<String, long[]> costModelMap = new HashMap<>();
        costModelMap.put(PLUTUS_V1, plutusV1Costs);

        protocolParams = ProtocolParams.builder()
                .adaPerUtxoByte(lovelacePerUTxOWord)
                .priceMem(priceMem)
                .priceStep(priceSteps)
                .maxTxExMem(txExUnitsMem)
                .maxTxExSteps(txExUnitsSteps)
                .maxBlockExMem(blockExUnitsMem)
                .maxBlockExSteps(blockExUnitsStep)
                .maxValSize(maxValueSize)
                .collateralPercent(collateralPercentage)
                .maxCollateralInputs(maxCollateralInputs)
                .costModels(costModelMap)
                .build();
    }

    @Override
    protected String getFileName() {
        return "alonzo-genesis.json";
    }
}

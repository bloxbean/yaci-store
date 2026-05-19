package com.bloxbean.cardano.yaci.store.submit.service;

import com.bloxbean.cardano.client.api.exception.ApiException;
import com.bloxbean.cardano.yaci.store.submit.service.scalus.ScalusTxEvaluationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vavr.control.Either;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Facade for transaction evaluation. It selects the configured evaluator and
 * normalizes evaluator responses to the Blockfrost-compatible v5 response
 * shape used by the submit endpoint.
 */
@Service
@RequiredArgsConstructor
public class TxEvaluationService {
    private final TxEvaluationProperties properties;
    private final ObjectProvider<OgmiosService> ogmiosService;
    private final ScalusTxEvaluationService scalusTxEvaluationService;
    private final ObjectMapper objectMapper;

    /**
     * Evaluates a CBOR transaction with the configured evaluator.
     *
     * @param cborTx transaction CBOR bytes
     * @param additionalUtxoSet optional additional UTxO set for Scalus evaluation
     * @return either evaluator error JSON or evaluator success JSON
     * @throws ApiException when the configured evaluator cannot be used
     */
    public Either<JsonNode, JsonNode> evaluateTx(byte[] cborTx, JsonNode additionalUtxoSet) throws ApiException {
        return switch (mode()) {
            case OGMIOS -> evaluateWithOgmios(cborTx);
            case SCALUS -> scalusTxEvaluationService.evaluateTx(cborTx, additionalUtxoSet);
        };
    }

    /**
     * Converts evaluator success JSON to the Blockfrost-compatible v5 Ogmios
     * response wrapper.
     *
     * @param jsonNode evaluator success JSON
     * @return v5-compatible success response
     */
    public JsonNode transformTxEvaluationSuccessResultToV5BFFormat(JsonNode jsonNode) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("type", "jsonwsp/response");
        response.put("version", "1.0");
        response.put("servicename", "ogmios");
        response.put("methodname", "EvaluateTx");

        ObjectNode resultNode = objectMapper.createObjectNode();
        ObjectNode evaluationResultsNode = objectMapper.createObjectNode();
        resultNode.set("EvaluationResult", evaluationResultsNode);

        JsonNode resultNodeFromEvaluator = jsonNode.get("result");
        if (resultNodeFromEvaluator instanceof ArrayNode result) {
            for (int i = 0; i < result.size(); i++) {
                JsonNode validatorResult = result.get(i);
                var validatorNode = validatorResult.get("validator");
                if (validatorNode == null)
                    continue;

                var index = validatorNode.get("index").asInt();
                var purpose = validatorNode.get("purpose").asText();

                var budgetNode = validatorResult.get("budget");
                if (budgetNode == null)
                    continue;

                var memory = budgetNode.get("memory").asLong();
                var cpu = budgetNode.get("cpu").asLong();

                ObjectNode cost = objectMapper.createObjectNode();
                cost.put("memory", memory);
                cost.put("steps", cpu);

                var tagIndex = purpose.toLowerCase() + ":" + index;
                evaluationResultsNode.set(tagIndex, cost);
            }
        }

        response.set("result", resultNode);

        return response;
    }

    /**
     * Converts evaluator error JSON to the Blockfrost-compatible v5 Ogmios
     * response wrapper.
     *
     * @param jsonNode evaluator error JSON
     * @return v5-compatible error response
     */
    public JsonNode transformTxEvaluationErrorToV5BFFormat(JsonNode jsonNode) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("type", "jsonwsp/response");
        response.put("version", "1.0");
        response.put("servicename", "ogmios");
        response.put("methodname", "EvaluateTx");

        ObjectNode resultNode = objectMapper.createObjectNode();

        JsonNode errorNode = jsonNode.get("error");
        resultNode.set("EvaluationFailure", errorNode != null ? errorNode : jsonNode);

        response.set("result", resultNode);

        return response;
    }

    private Either<JsonNode, JsonNode> evaluateWithOgmios(byte[] cborTx) throws ApiException {
        OgmiosService service = ogmiosService.getIfAvailable();
        if (service == null) {
            throw new ApiException("Ogmios tx evaluator requested but store.cardano.ogmios-url is not configured");
        }

        return service.evaluateTx(cborTx, Collections.emptySet());
    }

    private TxEvaluatorMode mode() {
        return properties.getTxEvaluatorMode() != null ? properties.getTxEvaluatorMode() : TxEvaluatorMode.SCALUS;
    }
}

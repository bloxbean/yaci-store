package com.bloxbean.cardano.yaci.store.submit.service.scalus;

import com.bloxbean.cardano.client.api.exception.ApiException;
import com.bloxbean.cardano.client.api.model.EvaluationResult;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.plutus.spec.RedeemerTag;
import com.bloxbean.cardano.yaci.store.client.epoch.EpochParamClient;
import com.bloxbean.cardano.yaci.store.client.utxo.DummyUtxoClient;
import com.bloxbean.cardano.yaci.store.client.utxo.UtxoClient;
import com.bloxbean.cardano.yaci.store.submit.domain.OgmiosUtxo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vavr.control.Either;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import scalus.bloxbean.EvaluatorMode;
import scalus.bloxbean.ScalusTransactionEvaluator;

import java.util.List;
import java.util.Set;

/**
 * Scalus-backed transaction evaluator for the submit module.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScalusTxEvaluationService {
    private final ObjectProvider<EpochParamClient> epochParamClient;
    private final ObjectProvider<UtxoClient> utxoClient;
    private final ScalusSlotConfigProvider slotConfigProvider;
    private final ObjectMapper objectMapper;

    /**
     * Evaluates a CBOR transaction with Scalus using local protocol parameters,
     * UTxO lookup and reference-script resolution.
     *
     * @param cborTx transaction CBOR bytes
     * @param additionalUtxoSet optional additional UTxO set from the request body
     * @return either evaluator error JSON or evaluator success JSON
     * @throws ApiException when required local clients or chain parameters are unavailable
     */
    public Either<JsonNode, JsonNode> evaluateTx(byte[] cborTx, JsonNode additionalUtxoSet) throws ApiException {
        if (log.isDebugEnabled())
            log.debug("Evaluating tx with Scalus");

        EpochParamClient epochParamClient = this.epochParamClient.getIfAvailable();
        if (epochParamClient == null)
            throw new ApiException("Scalus tx evaluator requires EpochParamClient. Enable the epoch store module locally.");

        UtxoClient utxoClient = this.utxoClient.getIfAvailable();
        if (utxoClient == null)
            throw new ApiException("Scalus tx evaluator requires UtxoClient. Enable UTxO APIs locally or configure store.utxo-client-url.");
        if (utxoClient instanceof DummyUtxoClient)
            throw new ApiException("Scalus tx evaluator requires a real UtxoClient. Enable UTxO APIs locally or configure store.utxo-client-url.");

        var protocolParams = epochParamClient.getLatestProtocolParams()
                .orElseThrow(() -> new ApiException("Latest protocol parameters are not available"));

        ReferenceScriptSupplier scriptSupplier = new ReferenceScriptSupplier();
        Set<Utxo> additionalUtxos = ScalusAdditionalUtxoMapper.fromAdditionalUtxoSet(OgmiosUtxo.fromAdditionalUtxoSet(additionalUtxoSet), scriptSupplier);
        StoreUtxoSupplier utxoSupplier = new StoreUtxoSupplier(utxoClient, scriptSupplier);

        ScalusTransactionEvaluator evaluator = new ScalusTransactionEvaluator(
                slotConfigProvider.getSlotConfig(),
                protocolParams,
                utxoSupplier,
                scriptSupplier,
                EvaluatorMode.EVALUATE_AND_COMPUTE_COST,
                false
        );

        Result<List<EvaluationResult>> result = evaluator.evaluateTx(cborTx, additionalUtxos);
        if (result.isSuccessful()) {
            return Either.right(toJson(result.getValue()));
        } else {
            return Either.left(toErrorJson(result.getResponse()));
        }
    }

    JsonNode toJson(List<EvaluationResult> evaluationResults) {
        ObjectNode response = objectMapper.createObjectNode();
        ArrayNode result = response.putArray("result");

        if (evaluationResults == null)
            return response;

        for (EvaluationResult evaluationResult : evaluationResults) {
            ObjectNode validatorResult = result.addObject();

            ObjectNode validator = validatorResult.putObject("validator");
            validator.put("purpose", purpose(evaluationResult.getRedeemerTag()));
            validator.put("index", evaluationResult.getIndex());

            ObjectNode budget = validatorResult.putObject("budget");
            if (evaluationResult.getExUnits() != null) {
                budget.put("memory", evaluationResult.getExUnits().getMem());
                budget.put("cpu", evaluationResult.getExUnits().getSteps());
            }
        }

        return response;
    }

    private JsonNode toErrorJson(String message) {
        ObjectNode response = objectMapper.createObjectNode();
        ObjectNode error = response.putObject("error");
        error.put("message", message != null ? message : "Transaction evaluation failed");
        return response;
    }

    String purpose(RedeemerTag redeemerTag) {
        if (redeemerTag == null)
            return "";

        return switch (redeemerTag) {
            case Spend -> "spend";
            case Mint -> "mint";
            // Ogmios' current schema uses publish/withdraw/vote/propose. Keep the
            // Scalus JSON aligned so the shared v5 formatter returns the same keys.
            case Cert -> "publish";
            case Reward -> "withdraw";
            case Voting -> "vote";
            case Proposing -> "propose";
        };
    }
}

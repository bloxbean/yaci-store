package com.bloxbean.cardano.yaci.store.submit.controller;

import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.store.common.util.JsonUtil;
import com.bloxbean.cardano.yaci.store.submit.service.TxEvaluationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Utilities")
@RestController
@RequestMapping("${apiPrefix}/utils/txs")
@RequiredArgsConstructor
@Slf4j
public class TxUtilController {

    private final TxEvaluationService txEvaluationService;

    @PostMapping(value = "evaluate", consumes = {MediaType.APPLICATION_CBOR_VALUE})
    @Operation(description = "Evaluate a CBOR encoded transaction. Returns the evaluation result")
    public ResponseEntity<?> evaluateTx(@RequestBody String cborTx,
                                             @RequestParam(value = "version",defaultValue = "5")
                                             @Parameter(description = "Optional parameter to specify the version of the Ogmios service to use. Default is 5. Set to 6 to use Ogmios version 6.")
                                             int version) {
        EvaluateRequest evaluateRequest = new EvaluateRequest();
        evaluateRequest.setCbor(cborTx);
        evaluateRequest.setAdditionalUtxoSet(NullNode.getInstance());
        return doEvaluateTx(evaluateRequest, version);
    }

    @PostMapping(value = "evaluate/utxos",
            consumes = {MediaType.APPLICATION_JSON_VALUE})
    @Operation(description = "Evaluate a CBOR encoded transaction. Returns the evaluation result. " +
            "additionalUtxoSet must use the Blockfrost/Ogmios [TxIn, TxOut] tuple format and is honored when the Scalus evaluator is configured. " +
            "The Ogmios evaluator keeps the existing behavior and ignores additional utxos.")
    public ResponseEntity<?> evaluateTx(@RequestBody EvaluateRequest evaluateRequest,
                                             @RequestParam(value = "version",defaultValue = "5")
                                             @Parameter(description = "Optional parameter to specify the version of the Ogmios service to use. Default is 5. Set to 6 to use Ogmios version 6.")
                                             int version) {
        return doEvaluateTx(evaluateRequest, version);
    }

    private ResponseEntity<?> doEvaluateTx(EvaluateRequest evaluateRequest, int version) {
        try {
            if (log.isDebugEnabled())
                log.debug("Evaluating tx : " + evaluateRequest);

            var cborBytes = HexUtil.decodeHexString(evaluateRequest.cbor);
            var response = txEvaluationService.evaluateTx(cborBytes, evaluateRequest.additionalUtxoSet);

            if (log.isDebugEnabled()) {
                log.debug("Original EvaluateTx response : " + response);
            }

            if (response.isRight()) {
                JsonNode successRes = null;
                if (version == 0 || version == 5) {
                    successRes = txEvaluationService.transformTxEvaluationSuccessResultToV5BFFormat(response.get());
                } else {
                    successRes = response.get();
                }

                return ResponseEntity.accepted()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(JsonUtil.getPrettyJson(successRes));
            } else if (response.isLeft()) {
                JsonNode failureRes = null;
                if (version == 0 || version == 5) {
                    failureRes = txEvaluationService.transformTxEvaluationErrorToV5BFFormat(response.getLeft());
                } else {
                    failureRes = response.getLeft();
                }

                TxErrorResponse errorResponse = new TxErrorResponse(400, "Bad Request", JsonUtil.getPrettyJson(failureRes));
                return ResponseEntity.badRequest().body(errorResponse);
            } else {
                return ResponseEntity.badRequest()
                        .body("Error evaluating tx");
            }

        } catch (IllegalArgumentException e) {
            if (log.isDebugEnabled()) {
                log.error("Invalid tx evaluation request: ", e);
            }
            TxErrorResponse errorResponse = new TxErrorResponse(400, "Bad Request", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.error("Error evaluating tx: ", e);
            }
            TxErrorResponse errorResponse = new TxErrorResponse(500, "Internal Server Error", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<TxErrorResponse> handleBadRequest(Exception e) {
        if (log.isDebugEnabled())
            log.error("Invalid request", e);
        TxErrorResponse errorResponse = new TxErrorResponse(400, "Bad Request", e.getMessage());
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<TxErrorResponse> handleException(Exception e) {
        if (log.isDebugEnabled())
            log.error("Unhandled exception", e);
        TxErrorResponse errorResponse = new TxErrorResponse(500, "Internal Server Error", e.getMessage());
        return ResponseEntity.status(500).body(errorResponse);
    }

    /**
     * Request body for the Blockfrost-compatible transaction evaluation endpoint.
     */
    @Data
    @NoArgsConstructor
    public static class EvaluateRequest {
        /**
         * Hex-encoded CBOR transaction.
         */
        private String cbor;

        /**
         * Optional additional UTxO set used by the Scalus evaluator. The value is
         * kept as JSON so the Ogmios/Blockfrost tuple shape can be parsed only by
         * the evaluator path that supports it.
         */
        private JsonNode additionalUtxoSet = NullNode.getInstance();
    }
}

package com.bloxbean.cardano.yaci.store.submit.controller;

import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.store.common.util.JsonUtil;
import com.bloxbean.cardano.yaci.store.submit.service.OgmiosService;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Set;

@Tag(name = "Utilities")
@RestController
@RequestMapping("${apiPrefix}/utils/txs")
@RequiredArgsConstructor
@ConditionalOnBean(OgmiosService.class)
@Slf4j
public class TxUtilController {

    private final OgmiosService ogmiosService;

    @PostMapping(value = "evaluate", consumes = {MediaType.APPLICATION_CBOR_VALUE})
    @Operation(description = "Evaluate a CBOR encoded transaction. Returns the evaluation result")
    public ResponseEntity<?> evaluateTx(@RequestBody String cborTx,
                                             @RequestParam(value = "version",defaultValue = "5")
                                             @Parameter(description = "Optional parameter to specify the version of the Ogmios service to use. Default is 5. Set to 6 to use Ogmios version 6.")
                                             int version) {
        EvaluateRequest evaluateRequest = new EvaluateRequest();
        evaluateRequest.setCbor(cborTx);
        evaluateRequest.setAdditionalUtxoSet(Collections.emptySet());
        return doEvaluateTx(evaluateRequest, version);
    }

    @PostMapping(value = "evaluate/utxos",
            consumes = {MediaType.APPLICATION_JSON_VALUE})
    @Operation(description = "Evaluate a CBOR encoded transaction. Returns the evaluation result. " +
            "Though additional utxos can be provided, it is not currently used in the implementation. " +
            "It is there for compatibility with the Blockfrost API")
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
            var response = ogmiosService.evaluateTx(cborBytes, Collections.emptySet());

            if (log.isDebugEnabled()) {
                log.debug("Original EvaluteTx reponse from Ogmios : " + response);
            }

            if (response.isRight()) {
                JsonNode successRes = null;
                if (version == 0 || version == 5) {
                    successRes = ogmiosService.transformTxEvaluationSuccessResultToV5BFFormat(response.get());
                } else {
                    successRes = response.get();
                }

                return ResponseEntity.accepted()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(JsonUtil.getPrettyJson(successRes));
            } else if (response.isLeft()) {
                JsonNode failureRes = null;
                if (version == 0 || version == 5) {
                    failureRes = ogmiosService.transformTxEvaluationErrorToV5BFFormat(response.getLeft());
                } else {
                    failureRes = response.getLeft();
                }

                TxErrorResponse errorResponse = new TxErrorResponse(400, "Bad Request", JsonUtil.getPrettyJson(failureRes));
                return ResponseEntity.badRequest().body(errorResponse);
            } else {
                return ResponseEntity.badRequest()
                        .body("Error evaluating tx");
            }

        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.error("Error evaluating tx: ", e);
            }
            TxErrorResponse errorResponse = new TxErrorResponse(500, "Internal Server Error", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<TxErrorResponse> handleException(Exception e) {
        if (log.isDebugEnabled())
            log.error("Unhandled exception", e);
        TxErrorResponse errorResponse = new TxErrorResponse(500, "Internal Server Error", e.getMessage());
        return ResponseEntity.status(500).body(errorResponse);
    }

    @Data
    @NoArgsConstructor
    public static class EvaluateRequest {
        private String cbor;
        private Set additionalUtxoSet;
    }
}

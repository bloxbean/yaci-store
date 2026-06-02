package com.bloxbean.cardano.yaci.store.blockfrost.util.controller;

import com.bloxbean.cardano.yaci.store.blockfrost.util.dto.BFEvaluateUtxosRequest;
import com.bloxbean.cardano.yaci.store.blockfrost.util.service.BFUtilService;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Blockfrost Utilities")
@RequestMapping("${blockfrost.apiPrefix}/utils")
@ConditionalOnExpression("${store.extensions.blockfrost.util.enabled:false}")
public class BFUtilController {

    private final BFUtilService bfUtilService;

    @PostConstruct
    public void postConstruct() {
        log.info("Blockfrost UtilController initialized >>>");
    }

    @PostMapping(value = "/txs/evaluate", consumes = MediaType.APPLICATION_CBOR_VALUE)
    @Operation(summary = "Submit a transaction for execution units evaluation",
               description = "Submit a CBOR-encoded transaction to evaluate execution units.")
    public ResponseEntity<JsonNode> evaluateTx(@RequestBody byte[] cborTx,
                                               @RequestParam(name = "version", defaultValue = "5") int version) {
        JsonNode result = bfUtilService.evaluateTx(cborTx, version);
        return ResponseEntity.ok(result);
    }

    @PostMapping(value = "/txs/evaluate/utxos", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Submit a transaction for execution units evaluation (additional UTXO set)",
               description = "Submit a JSON payload with transaction CBOR and additional UTXO set to evaluate execution units.")
    public ResponseEntity<JsonNode> evaluateTxWithUtxos(@RequestBody BFEvaluateUtxosRequest request,
                                                        @RequestParam(name = "version", defaultValue = "5") int version) {
        JsonNode result = bfUtilService.evaluateTxWithUtxos(request.getCbor(), request.getAdditionalUtxoSet(), version);
        return ResponseEntity.ok(result);
    }
}

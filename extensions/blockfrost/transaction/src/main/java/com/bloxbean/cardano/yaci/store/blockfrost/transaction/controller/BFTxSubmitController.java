package com.bloxbean.cardano.yaci.store.blockfrost.transaction.controller;

import com.bloxbean.cardano.yaci.store.blockfrost.transaction.service.BFTxSubmitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Blockfrost Transactions")
@RequestMapping("${blockfrost.apiPrefix}/tx")
@ConditionalOnExpression("${store.extensions.blockfrost.transaction.enabled:true}")
public class BFTxSubmitController {

    private final BFTxSubmitService bfTxSubmitService;

    @PostConstruct
    public void postConstruct() {
        log.info("Blockfrost TxSubmitController initialized >>>");
    }

    @PostMapping(value = "submit", consumes = MediaType.APPLICATION_CBOR_VALUE)
    @Operation(summary = "Submit a transaction",
               description = "Submit an already serialized transaction to the network.")
    public ResponseEntity<String> submitTx(@RequestBody byte[] cborTx) {
        String txHash = bfTxSubmitService.submitTx(cborTx);
        return ResponseEntity.ok(txHash);
    }
}

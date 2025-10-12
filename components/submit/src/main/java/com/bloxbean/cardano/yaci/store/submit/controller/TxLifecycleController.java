package com.bloxbean.cardano.yaci.store.submit.controller;

import com.bloxbean.cardano.yaci.core.util.HexUtil;
import com.bloxbean.cardano.yaci.store.common.domain.Cursor;
import com.bloxbean.cardano.yaci.store.common.service.CursorService;
import com.bloxbean.cardano.yaci.store.submit.domain.SubmittedTransaction;
import com.bloxbean.cardano.yaci.store.submit.domain.TxStatus;
import com.bloxbean.cardano.yaci.store.submit.service.SmartTxSubmissionService;
import com.bloxbean.cardano.yaci.store.submit.service.TxLifecycleService;
import com.bloxbean.cardano.yaci.store.submit.storage.impl.mapper.SubmittedTransactionMapper;
import com.bloxbean.cardano.yaci.store.submit.storage.impl.repository.SubmittedTransactionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * REST API controller for smart transaction submission with lifecycle tracking.
 */
@RestController
@Tag(name = "Transaction Lifecycle Service")
@RequestMapping("${apiPrefix}/tx/lifecycle")
@ConditionalOnProperty(
        prefix = "store.submit.lifecycle",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
@RequiredArgsConstructor
@Slf4j
public class TxLifecycleController {
    
    private final SmartTxSubmissionService smartSubmissionService;
    private final TxLifecycleService lifecycleService;
    private final CursorService cursorService;
    
    /**
     * Submit transaction with lifecycle tracking (CBOR format).
     */
    @PostMapping(value = "/submit", consumes = {MediaType.APPLICATION_CBOR_VALUE})
    @Operation(summary = "Submit transaction with lifecycle tracking (CBOR)")
    public ResponseEntity<SubmittedTransaction> submitTxCbor(@RequestBody byte[] txBytes) {
        SubmittedTransaction result = smartSubmissionService.submitTransaction(txBytes);
        
        if (result.getStatus() == TxStatus.FAILED) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
        
        return ResponseEntity.accepted().body(result);
    }
    
    /**
     * Submit transaction with lifecycle tracking (Hex string format).
     */
    @PostMapping(value = "/submit", consumes = {MediaType.TEXT_PLAIN_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(summary = "Submit transaction with lifecycle tracking (Hex)")
    public ResponseEntity<SubmittedTransaction> submitTxHex(@RequestBody String txBytesHex) {
        
        byte[] txBytes = HexUtil.decodeHexString(txBytesHex);
        return submitTxCbor(txBytes);
    }
    
    /**
     * Get transaction status by txHash.
     */
    @GetMapping("/{txHash}")
    @Operation(summary = "Get transaction status and details")
    public ResponseEntity<TxStatusResponse> getTransactionStatus(@PathVariable String txHash) {
        Optional<SubmittedTransaction> txOpt = lifecycleService.getTransaction(txHash);

        if (txOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        SubmittedTransaction tx = txOpt.get();

        // Calculate confirmations if possible
        Integer confirmations = null;
        Optional<Cursor> cursorOpt = cursorService.getCursor();
        if (cursorOpt.isPresent()) {
            confirmations = tx.getConfirmations(cursorOpt.get().getBlock());
        }

        TxStatusResponse response = TxStatusResponse.builder()
                .txHash(tx.getTxHash())
                .status(tx.getStatus())
                .submittedAt(tx.getSubmittedAt())
                .confirmedAt(tx.getConfirmedAt())
                .confirmedSlot(tx.getConfirmedSlot())
                .confirmedBlockNumber(tx.getConfirmedBlockNumber())
                .confirmations(confirmations)
                .successAt(tx.getSuccessAt())
                .finalizedAt(tx.getFinalizedAt())
                .errorMessage(tx.getErrorMessage())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Response DTO for transaction status.
     */
    @Data
    @Builder
    @AllArgsConstructor
    public static class TxStatusResponse {
        private String txHash;
        private TxStatus status;
        private java.sql.Timestamp submittedAt;
        private java.sql.Timestamp confirmedAt;
        private Long confirmedSlot;
        private Long confirmedBlockNumber;
        private Integer confirmations;
        private java.sql.Timestamp successAt;
        private java.sql.Timestamp finalizedAt;
        private String errorMessage;
    }
}


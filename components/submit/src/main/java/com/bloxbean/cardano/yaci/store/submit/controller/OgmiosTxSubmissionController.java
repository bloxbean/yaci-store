package com.bloxbean.cardano.yaci.store.submit.controller;

import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.yaci.core.util.HexUtil;
import com.bloxbean.cardano.yaci.store.submit.service.OgmiosService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Ogmios Tx Submission Service")
@RequestMapping("${apiPrefix}/tx")
@ConditionalOnBean(OgmiosService.class)
@ConditionalOnMissingBean(TxSubmitController.class)
@Slf4j
public class OgmiosTxSubmissionController {
    private final OgmiosService ogmiosService;

    public OgmiosTxSubmissionController(OgmiosService ogmiosService) {
        this.ogmiosService = ogmiosService;
        log.info("<< Ogmios Tx Submission Controller initialized >>");
    }

    @PostMapping(value = "submit", consumes = {MediaType.APPLICATION_CBOR_VALUE})
    public ResponseEntity<String> submitTx(@RequestBody byte[] txBytes) {
        return ogmiosTxSubmission(txBytes);
    }

    @PostMapping(value = "submit", consumes = {MediaType.TEXT_PLAIN_VALUE})
    public ResponseEntity<String> submitTx(@RequestBody String txBytesHex) {
        byte[] cborBytes = HexUtil.decodeHexString(txBytesHex);
        return ogmiosTxSubmission(cborBytes);
    }

    private ResponseEntity<String> ogmiosTxSubmission(byte[] cborTx) {
        try {
            Result<String> result = ogmiosService.submitTx(cborTx);
            if (result.isSuccessful()) {
                return ResponseEntity.accepted()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("\"" + result.getValue() + "\"");
            } else {
                return ResponseEntity.badRequest()
                        .body(result.getResponse());
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }
    }
}

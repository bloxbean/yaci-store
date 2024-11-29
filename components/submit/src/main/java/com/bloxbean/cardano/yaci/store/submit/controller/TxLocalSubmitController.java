package com.bloxbean.cardano.yaci.store.submit.controller;

import com.bloxbean.cardano.yaci.core.common.TxBodyType;
import com.bloxbean.cardano.yaci.core.util.HexUtil;
import com.bloxbean.cardano.yaci.helper.model.TxResult;
import com.bloxbean.cardano.yaci.store.submit.service.OgmiosService;
import com.bloxbean.cardano.yaci.store.submit.service.TxSubmissionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Local Tx Submission Service")
@RequestMapping("${apiPrefix}/tx")
@ConditionalOnBean(TxSubmissionService.class)
@ConditionalOnExpression("'${store.cardano.submit-api-url:}' == ''")
@ConditionalOnMissingBean(OgmiosService.class)
@Slf4j
public class TxLocalSubmitController {
    private final TxSubmissionService txSubmissionService;

    public TxLocalSubmitController(TxSubmissionService txSubmissionService) {
        this.txSubmissionService = txSubmissionService;
        log.info("<< Tx Local Submit Controller initialized >>");
    }

    @PostMapping(value = "submit", consumes = {MediaType.APPLICATION_CBOR_VALUE})
    public ResponseEntity<String> submitTx(@RequestBody byte[] txBytes) {
        TxResult txResult = txSubmissionService.submitTx(TxBodyType.CONWAY, txBytes);
        if (log.isDebugEnabled())
            log.debug(String.valueOf(txResult));

        if (txResult.isAccepted()) {
            return ResponseEntity.accepted()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("\"" + txResult.getTxHash() + "\"");
        } else
            return ResponseEntity.badRequest()
                    .body(txResult.getErrorCbor());
    }

    @PostMapping(value = "submit", consumes = {MediaType.TEXT_PLAIN_VALUE})
    public ResponseEntity<String> submitTx(@RequestBody String txBytesHex) {
        byte[] txBytes = HexUtil.decodeHexString(txBytesHex);
        TxResult txResult = txSubmissionService.submitTx(TxBodyType.CONWAY, txBytes);
        if (log.isDebugEnabled())
            log.debug(String.valueOf(txResult));

        if (txResult.isAccepted())
            return ResponseEntity.ok(txResult.getTxHash());
        else
            return ResponseEntity.badRequest()
                    .body(txResult.getErrorCbor());
    }

}

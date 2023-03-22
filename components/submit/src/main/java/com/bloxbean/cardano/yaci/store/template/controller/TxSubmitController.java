package com.bloxbean.cardano.yaci.store.template.controller;

import com.bloxbean.cardano.yaci.core.common.TxBodyType;
import com.bloxbean.cardano.yaci.core.util.HexUtil;
import com.bloxbean.cardano.yaci.helper.model.TxResult;
import com.bloxbean.cardano.yaci.store.template.service.TxSubmissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.*;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("${apiPrefix}/tx")
@RequiredArgsConstructor
@ConditionalOnBean(TxSubmissionService.class)
@Slf4j
public class TxSubmitController {
    private RestTemplate restTemplate = new RestTemplate();

    @Value("${store.cardano.submit-api-url:#{null}}")
    private String submitApiUrl;

    private final TxSubmissionService txSubmissionService;

    @PostMapping(value = "submit", consumes = {MediaType.APPLICATION_CBOR_VALUE})
    public ResponseEntity<String> submitTx(@RequestBody byte[] txBytes) {
        if (!StringUtils.hasLength(submitApiUrl)) {
            TxResult txResult = txSubmissionService.submitTx(TxBodyType.BABBAGE, txBytes);
            if (log.isDebugEnabled())
                log.debug(String.valueOf(txResult));

            if (txResult.isAccepted())
                return ResponseEntity.ok(txResult.getTxHash());
            else
                return ResponseEntity.badRequest()
                        .body(txResult.getErrorCbor());
        } else {
            return invokeSubmitApiUrl(txBytes);
        }
    }

    @PostMapping(value = "submit", consumes = {MediaType.TEXT_PLAIN_VALUE})
    public ResponseEntity<String> submitTx(@RequestBody String txBytesHex) {
        byte[] txBytes = HexUtil.decodeHexString(txBytesHex);
        if (!StringUtils.hasLength(submitApiUrl)) {
            TxResult txResult = txSubmissionService.submitTx(TxBodyType.BABBAGE, txBytes);
            if (log.isDebugEnabled())
                log.debug(String.valueOf(txResult));

            if (txResult.isAccepted())
                return ResponseEntity.ok(txResult.getTxHash());
            else
                return ResponseEntity.badRequest()
                        .body(txResult.getErrorCbor());
        } else {
            return invokeSubmitApiUrl(txBytes);
        }
    }

    ResponseEntity<String> invokeSubmitApiUrl(byte[] cborTx) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/cbor");

        HttpEntity<byte[]> entity = new HttpEntity<>(cborTx, headers);
        try {
            ResponseEntity<String> responseEntity = restTemplate
                    .exchange(submitApiUrl, HttpMethod.POST, entity, String.class);

            return responseEntity;
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }
    }
}

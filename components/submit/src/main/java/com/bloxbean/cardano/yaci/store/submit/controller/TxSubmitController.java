package com.bloxbean.cardano.yaci.store.submit.controller;

import com.bloxbean.cardano.yaci.core.util.HexUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@Tag(name = "Tx Submission Service")
@RequestMapping("${apiPrefix}/tx")
@RequiredArgsConstructor
@ConditionalOnExpression("'${store.cardano.submit-api-url:}' != ''")
@Slf4j
public class TxSubmitController {
    private RestTemplate restTemplate = new RestTemplate();

    @Value("${store.cardano.submit-api-url:#{null}}")
    private String submitApiUrl;

    @PostConstruct
    public void postConstruct() {
        log.info("Tx Submit Controller (Submit Api) initialized");
    }

    @PostMapping(value = "submit", consumes = {MediaType.APPLICATION_CBOR_VALUE})
    public ResponseEntity<String> submitTx(@RequestBody byte[] txBytes) {
            return invokeSubmitApiUrl(txBytes);

    }

    @PostMapping(value = "submit", consumes = {MediaType.TEXT_PLAIN_VALUE})
    public ResponseEntity<String> submitTx(@RequestBody String txBytesHex) {
        byte[] txBytes = HexUtil.decodeHexString(txBytesHex);
            return invokeSubmitApiUrl(txBytes);
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

package com.bloxbean.cardano.yaci.store.submit.controller;

import com.bloxbean.cardano.yaci.core.util.HexUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@Tag(name = "Tx Submission Service")
@RequestMapping("${apiPrefix}/tx")
@ConditionalOnExpression("'${store.cardano.submit-api-url:}' != ''")
@Slf4j
public class TxSubmitController {
    private RestTemplate restTemplate = new RestTemplate();

    private String submitApiUrl;

    public TxSubmitController(Environment env) {
        this.submitApiUrl = env.getProperty("store.cardano.submit-api-url");
        log.info("<< Tx Submit Controller (Submit Api) initialized >> " + submitApiUrl);
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
            if (log.isDebugEnabled())
                log.debug("Submitting tx to : " + submitApiUrl);
            ResponseEntity<String> responseEntity = restTemplate
                    .exchange(submitApiUrl, HttpMethod.POST, entity, String.class);

            return responseEntity;
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }
    }
}

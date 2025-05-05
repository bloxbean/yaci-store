package com.bloxbean.cardano.yaci.store.submit.controller;

import com.bloxbean.cardano.yaci.core.util.HexUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
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
    public ResponseEntity<?> submitTx(@RequestBody byte[] txBytes) {
        if (log.isDebugEnabled())
            log.debug("Submitting tx to : " + submitApiUrl);

        return invokeSubmitApiUrl(txBytes);

    }

    @PostMapping(value = "submit", consumes = {MediaType.TEXT_PLAIN_VALUE})
    public ResponseEntity<?> submitTx(@RequestBody String txBytesHex) {
        if (log.isDebugEnabled())
            log.debug("Submitting tx to : " + submitApiUrl);
        byte[] txBytes = HexUtil.decodeHexString(txBytesHex);
            return invokeSubmitApiUrl(txBytes);
    }

    ResponseEntity<?> invokeSubmitApiUrl(byte[] cborTx) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/cbor");

        HttpEntity<byte[]> entity = new HttpEntity<>(cborTx, headers);
        try {
            if (log.isDebugEnabled())
                log.debug("Submitting tx to : " + submitApiUrl);
            ResponseEntity<String> responseEntity = restTemplate
                    .exchange(submitApiUrl, HttpMethod.POST, entity, String.class);

            return responseEntity;
        }  catch (HttpClientErrorException | HttpServerErrorException e) {
            int statusCode = e.getStatusCode().value();
            String error = e.getStatusCode().toString();
            String message = e.getResponseBodyAsString();

            if (log.isDebugEnabled())
                log.debug("Error submitting tx: Status = {} , Error = {} , Message = {}", statusCode, error, message);

            TxErrorResponse errorResponse = new TxErrorResponse(statusCode, error, message);
            return ResponseEntity.status(statusCode).body(errorResponse);
        } catch (Exception e) {
            if (log.isDebugEnabled())
                log.error("Unexpected error submitting tx", e);
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
}

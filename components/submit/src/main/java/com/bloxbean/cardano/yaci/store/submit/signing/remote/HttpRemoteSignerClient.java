package com.bloxbean.cardano.yaci.store.submit.signing.remote;

import com.bloxbean.cardano.client.util.HexUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Default HTTP-based RemoteSignerClient.
 * Request JSON: {keyId, scope, txBody, address?, verificationKey?}
 * Response JSON: {signature, verificationKey?}
 */
@Slf4j
public class HttpRemoteSignerClient implements RemoteSignerClient {

    private static final String DEFAULT_PATH = "/sign";
    private final RestTemplateBuilder restTemplateBuilder;
    private final RestTemplate fixedRestTemplate;

    public HttpRemoteSignerClient(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplateBuilder = restTemplateBuilder;
        this.fixedRestTemplate = null;
    }

    /**
     * Testing-only constructor to inject a preconfigured RestTemplate.
     */
    public HttpRemoteSignerClient(RestTemplate restTemplate) {
        this.restTemplateBuilder = new RestTemplateBuilder();
        this.fixedRestTemplate = restTemplate;
    }

    @Override
    public RemoteSignerResponse sign(RemoteSignerRequest request) throws RemoteSignerException {
        if (!StringUtils.hasText(request.getEndpoint())) {
            throw new RemoteSignerException("Remote signer endpoint is required for ref=%s".formatted(request.getRef()));
        }
        if (!StringUtils.hasText(request.getKeyId())) {
            throw new RemoteSignerException("Remote signer keyId is required for ref=%s".formatted(request.getRef()));
        }

        RestTemplate restTemplate = buildRestTemplate(request.getTimeoutMs());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (StringUtils.hasText(request.getAuthToken())) {
            headers.setBearerAuth(request.getAuthToken());
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("keyId", request.getKeyId());
        payload.put("scope", request.getScope());
        payload.put("txBody", HexUtil.encodeHexString(request.getTxBody()));
        if (StringUtils.hasText(request.getAddress())) {
            payload.put("address", request.getAddress());
        }
        if (StringUtils.hasText(request.getVerificationKey())) {
            payload.put("verificationKey", request.getVerificationKey());
        }

        URI uri = buildUri(request.getEndpoint());

        try {
            ResponseEntity<SignResponse> responseEntity = restTemplate.postForEntity(
                    uri,
                    new HttpEntity<>(payload, headers),
                    SignResponse.class
            );
            SignResponse body = responseEntity.getBody();
            if (!responseEntity.getStatusCode().is2xxSuccessful() || body == null) {
                throw new RemoteSignerException("Remote signer ref=%s returned %s".formatted(
                        request.getRef(), responseEntity.getStatusCode()));
            }

            byte[] signature = decodeHex(body.getSignature(), "signature", request.getRef());
            byte[] vkey = decodeHex(body.getVerificationKey(), "verificationKey", request.getRef());
            return new RemoteSignerResponse(signature, vkey);
        } catch (RemoteSignerException e) {
            throw e;
        } catch (Exception e) {
            throw new RemoteSignerException("Remote signer call failed for ref=%s: %s".formatted(
                    request.getRef(), e.getMessage()), e);
        }
    }

    private RestTemplate buildRestTemplate(Integer timeoutMs) {
        if (fixedRestTemplate != null) {
            return fixedRestTemplate;
        }
        if (timeoutMs == null || timeoutMs <= 0) {
            return restTemplateBuilder.build();
        }
        return restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(timeoutMs))
                .setReadTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }

    private URI buildUri(String endpoint) {
        if (endpoint.endsWith("/sign")) {
            return URI.create(endpoint);
        }
        return URI.create(endpoint + DEFAULT_PATH);
    }

    private byte[] decodeHex(String value, String field, String ref) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return HexUtil.decodeHexString(value);
        } catch (Exception e) {
            throw new RemoteSignerException("Invalid hex in %s for ref=%s".formatted(field, ref), e);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class SignResponse {
        private String signature;
        private String verificationKey;
    }
}

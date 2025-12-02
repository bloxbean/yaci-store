package com.bloxbean.cardano.yaci.store.submit.dev;

import com.bloxbean.cardano.client.util.HexUtil;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 * Simple stub signer for local development. Not chain-valid.
 * Endpoint: POST /stub-remote-signer/sign
 * Request JSON: {keyId, scope, txBody, address?, verificationKey?}
 * Response JSON: {signature, verificationKey}
 */
@RestController
@RequestMapping("/stub-remote-signer")
public class StubRemoteSignerController {

    @PostMapping(value = "/sign", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> sign(@RequestBody SignRequest request) {
        byte[] bodyBytes = HexUtil.decodeHexString(request.txBody());
        String signatureHex = HexUtil.encodeHexString(hash(bodyBytes));

        String verificationKey = request.verificationKey();
        if (verificationKey == null || verificationKey.isBlank()) {
            verificationKey = "00".repeat(32); // placeholder vkey
        }

        return Map.of(
                "signature", signatureHex,
                "verificationKey", verificationKey
        );
    }

    private byte[] hash(byte[] input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(input != null ? input : "stub".getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            // Fallback deterministic bytes
            return "stub".getBytes(StandardCharsets.UTF_8);
        }
    }

    public record SignRequest(String keyId, String scope, String txBody, String address, String verificationKey) {}
}

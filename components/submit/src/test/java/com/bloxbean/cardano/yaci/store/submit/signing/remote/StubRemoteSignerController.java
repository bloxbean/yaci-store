package com.bloxbean.cardano.yaci.store.submit.signing.remote;

import com.bloxbean.cardano.client.util.HexUtil;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * Stub remote signer for local testing/dev. Not packaged in production.
 * Echoes a deterministic signature derived from txBody hex (not chain-valid).
 */
@RestController
@RequestMapping("/stub-remote-signer")
public class StubRemoteSignerController {

    @PostMapping(value = "/sign", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public SignResponse sign(@RequestBody SignRequest request) {
        // Create a deterministic but fake signature for testing purposes.
        byte[] body = HexUtil.decodeHexString(request.txBody());
        String signatureHex = HexUtil.encodeHexString(hashStub(body));

        // Prefer provided verificationKey if present; otherwise echo a placeholder.
        String verificationKey = request.verificationKey();
        if (verificationKey == null || verificationKey.isBlank()) {
            verificationKey = "00".repeat(32); // placeholder vkey
        }

        return new SignResponse(signatureHex, verificationKey);
    }

    private byte[] hashStub(byte[] body) {
        // Extremely simple checksum to avoid pulling extra deps in tests
        int sum = 0;
        for (byte b : body) {
            sum = (sum + (b & 0xff)) & 0xffff;
        }
        return (sum + "-stub").getBytes(StandardCharsets.UTF_8);
    }

    public record SignRequest(String keyId, String scope, String txBody, String address, String verificationKey) {}
    public record SignResponse(String signature, String verificationKey) {}
}

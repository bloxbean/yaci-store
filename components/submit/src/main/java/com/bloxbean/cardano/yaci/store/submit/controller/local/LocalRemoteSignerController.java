package com.bloxbean.cardano.yaci.store.submit.controller.local;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.crypto.Blake2bUtil;
import com.bloxbean.cardano.client.crypto.bip32.HdKeyPair;
import com.bloxbean.cardano.client.crypto.cip1852.DerivationPath;
import com.bloxbean.cardano.client.crypto.config.CryptoConfiguration;
import com.bloxbean.cardano.client.crypto.api.SigningProvider;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.client.util.Tuple;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Local signer for local development and E2E tests.
 * Creates REAL, chain-valid Ed25519 signatures using test account keys.
 *
 * Uses the standard test mnemonic and creates different accounts for different key IDs:
 * - ops-key-1: account 0 (payment scope)
 * - stake-key-1: account 1 (stake scope)
 * - policy-key-1: account 2 (policy scope)
 * - payment-key-1: account 3 (payment scope)
 *
 * Endpoint: POST /local-remote-signer/sign
 * Request JSON: {keyId, scope, txBody, address?, verificationKey?}
 * Response JSON: {signature, verificationKey}
 */
@Slf4j
@RestController
@RequestMapping("/local-remote-signer")
public class LocalRemoteSignerController {

    private final Map<String, Tuple<Account, String>> accountRegistry;

    public LocalRemoteSignerController() {
        accountRegistry = new HashMap<>();
        initializeAccounts();
    }

    private void initializeAccounts() {
        // Use the same mnemonic as TxBuilderSignerRegistryIT tests
        String testMnemonic = "test test test test test test test test test test test test test test test test test test test test test test test sauce";

        // Create accounts for different key IDs
        // Account 0 for ops-key-1 (payment scope)
        Account account0 = new Account(Networks.testnet(), testMnemonic);
        accountRegistry.put("ops-key-1", new Tuple<>(account0, "payment"));

        // Account 1 for stake-key-1 (stake scope)
        Account account1 = new Account(Networks.testnet(), testMnemonic, DerivationPath.createExternalAddressDerivationPathForAccount(1));
        accountRegistry.put("stake-key-1", new Tuple<>(account1, "stake"));

        // Account 2 for policy-key-1 (policy/payment scope)
        Account account2 = new Account(Networks.testnet(), testMnemonic, DerivationPath.createExternalAddressDerivationPathForAccount(2));
        accountRegistry.put("policy-key-1", new Tuple<>(account2, "payment"));

        // Account 3 for payment-key-1 (payment scope)
        Account account3 = new Account(Networks.testnet(), testMnemonic, DerivationPath.createExternalAddressDerivationPathForAccount(3));
        accountRegistry.put("payment-key-1", new Tuple<>(account3, "payment"));

        // Account 4 for unreachable-key (this will never be called but include for completeness)
        Account account4 = new Account(Networks.testnet(), testMnemonic, DerivationPath.createExternalAddressDerivationPathForAccount(4));
        accountRegistry.put("unreachable-key", new Tuple<>(account4, "payment"));
    }

    @PostMapping(value = "/sign", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> sign(@RequestBody SignRequest request) {
        try {
            // Get the account for this keyId
            Tuple<Account, String> accountInfo = accountRegistry.get(request.keyId());
            if (accountInfo == null) {
                throw new IllegalArgumentException("Unknown keyId: " + request.keyId());
            }

            Account account = accountInfo._1;
            String defaultScope = accountInfo._2;

            // Use the scope from request, or fall back to default
            String scope = request.scope() != null ? request.scope() : defaultScope;

            // Decode transaction body CBOR
            byte[] txBodyBytes = HexUtil.decodeHexString(request.txBody());

            // Extract the appropriate signing key based on scope
            var keyPair = getKeyPairForScope(account, scope);
            byte[] privateKey = keyPair.getPrivateKey().getKeyData();
            byte[] publicKey = keyPair.getPublicKey().getKeyData();

            // Hash the transaction body with Blake2b-256
            byte[] txBodyHash = Blake2bUtil.blake2bHash256(txBodyBytes);

            // Sign the hash with Ed25519
            SigningProvider signingProvider = CryptoConfiguration.INSTANCE.getSigningProvider();
            byte[] signature = signingProvider.signExtended(txBodyHash, privateKey, publicKey);

            // Return signature and verification key
            String signatureHex = HexUtil.encodeHexString(signature);
            String vkeyHex = HexUtil.encodeHexString(publicKey);

            // Calculate and log the verification key hash (for debugging)
            byte[] vkeyHash = com.bloxbean.cardano.client.crypto.Blake2bUtil.blake2bHash224(publicKey);
            String vkeyHashHex = HexUtil.encodeHexString(vkeyHash);

            log.debug("=== Stub Remote Signer ===");
            log.debug("  keyId: {}", request.keyId());
            log.debug("  scope: {}", scope);
            log.debug("  vkey: {}", vkeyHex);
            log.debug("  vkey hash: {}", vkeyHashHex);
            log.debug("  signature: {}...", signatureHex.substring(0, 32));
            log.debug("========================");

            return Map.of(
                "signature", signatureHex,
                "verificationKey", vkeyHex
            );

        } catch (Exception e) {
            log.error("Stub signer error: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to sign transaction", e);
        }
    }

    private HdKeyPair getKeyPairForScope(Account account, String scope) {
        String normalized = scope != null ? scope.toLowerCase() : "payment";

        return switch (normalized) {
            case "payment", "policy" -> account.hdKeyPair();
            case "stake" -> account.stakeHdKeyPair();
            case "drep" -> account.drepHdKeyPair();
            case "committeecold" -> account.committeeColdKeyPair();
            case "committeehot" -> account.committeeHotKeyPair();
            default -> {
                log.warn("Unknown scope: {}, using payment key", scope);
                yield account.hdKeyPair();
            }
        };
    }

    public record SignRequest(String keyId, String scope, String txBody, String address, String verificationKey) {}
}

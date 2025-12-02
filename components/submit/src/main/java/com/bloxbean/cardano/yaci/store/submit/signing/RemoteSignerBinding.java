package com.bloxbean.cardano.yaci.store.submit.signing;

import com.bloxbean.cardano.client.common.cbor.CborSerializationUtil;
import com.bloxbean.cardano.client.function.TxSigner;
import com.bloxbean.cardano.client.quicktx.signing.SignerBinding;
import com.bloxbean.cardano.client.transaction.spec.Transaction;
import com.bloxbean.cardano.client.transaction.spec.TransactionWitnessSet;
import com.bloxbean.cardano.client.transaction.spec.VkeyWitness;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.hdwallet.Wallet;
import com.bloxbean.cardano.yaci.store.submit.config.SubmitSignerRegistryProperties.RemoteSignerProperties;
import com.bloxbean.cardano.yaci.store.submit.signing.remote.RemoteSignerClient;
import com.bloxbean.cardano.yaci.store.submit.signing.remote.RemoteSignerRequest;
import com.bloxbean.cardano.yaci.store.submit.signing.remote.RemoteSignerResponse;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Binding that delegates signing to an external remote-signer service (HTTP in v1).
 */
public class RemoteSignerBinding implements SignerBinding {
    private final String ref;
    private final Set<String> allowedScopes;
    private final RemoteSignerProperties properties;
    private final RemoteSignerClient client;

    public RemoteSignerBinding(String ref,
                               Set<String> allowedScopes,
                               RemoteSignerProperties properties,
                               RemoteSignerClient client) {
        this.ref = ref;
        this.allowedScopes = allowedScopes;
        this.properties = properties;
        this.client = client;
    }

    @Override
    public TxSigner signerFor(String scope) {
        String normalized = normalize(scope);
        if (!CollectionUtils.isEmpty(allowedScopes) && !allowedScopes.contains(normalized)) {
            throw new IllegalArgumentException("Scope '%s' not allowed for ref '%s'".formatted(scope, ref));
        }

        return (ctx, tx) -> addRemoteWitness(normalized, tx);
    }

    @Override
    public Optional<Wallet> asWallet() {
        return Optional.empty();
    }

    @Override
    public Optional<String> preferredAddress() {
        return Optional.ofNullable(properties.getAddress());
    }

    private Transaction addRemoteWitness(String scope, Transaction tx) {
        byte[] txBody = serializeBody(tx);

        RemoteSignerRequest request = RemoteSignerRequest.builder()
                .ref(ref)
                .scope(scope)
                .keyId(properties.getKeyId())
                .txBody(txBody)
                .endpoint(properties.getEndpoint())
                .authToken(properties.getAuthToken())
                .verificationKey(properties.getVerificationKey())
                .address(properties.getAddress())
                .timeoutMs(properties.getTimeoutMs())
                .build();

        RemoteSignerResponse response = client.sign(request);
        byte[] signature = requireBytes(response.getSignature(), "signature");
        byte[] verificationKey = response.getVerificationKey();

        if (verificationKey == null || verificationKey.length == 0) {
            verificationKey = decodeHex(properties.getVerificationKey(), "verificationKey");
        }

        if (verificationKey == null || verificationKey.length == 0) {
            throw new IllegalStateException("Remote signer ref='%s' did not provide verification key".formatted(ref));
        }

        TransactionWitnessSet witnessSet = Optional.ofNullable(tx.getWitnessSet())
                .orElseGet(TransactionWitnessSet::new);
        var vkeyWitnesses = Optional.ofNullable(witnessSet.getVkeyWitnesses())
                .orElseGet(ArrayList::new);

        vkeyWitnesses.add(new VkeyWitness(verificationKey, signature));
        witnessSet.setVkeyWitnesses(vkeyWitnesses);
        tx.setWitnessSet(witnessSet);

        return tx;
    }

    private byte[] serializeBody(Transaction tx) {
        try {
            return CborSerializationUtil.serialize(tx.getBody().serialize());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize transaction body for ref '%s'".formatted(ref), e);
        }
    }

    private byte[] requireBytes(byte[] value, String name) {
        if (value == null || value.length == 0) {
            throw new IllegalStateException("Remote signer ref='%s' returned empty %s".formatted(ref, name));
        }

        return value;
    }

    private byte[] decodeHex(String value, String name) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        try {
            return HexUtil.decodeHexString(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid hex for %s in ref='%s'".formatted(name, ref), e);
        }
    }

    private String normalize(String scope) {
        if (scope == null) {
            return "";
        }

        return scope.trim().toLowerCase(Locale.ROOT);
    }
}

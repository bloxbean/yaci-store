package com.bloxbean.cardano.yaci.store.submit.signing;

import com.bloxbean.cardano.client.function.TxSigner;
import com.bloxbean.cardano.hdwallet.Wallet;
import com.bloxbean.cardano.client.quicktx.signing.SignerBinding;
import lombok.RequiredArgsConstructor;
import org.springframework.util.CollectionUtils;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Binding that only exposes an address (no signing). Useful for build-only flows.
 */
@RequiredArgsConstructor
public class AddressOnlySignerBinding implements SignerBinding {
    private final String ref;
    private final String address;
    private final Set<String> allowedScopes;

    @Override
    public TxSigner signerFor(String scope) {
        String normalized = normalize(scope);
        if (!CollectionUtils.isEmpty(allowedScopes) && !allowedScopes.contains(normalized)) {
            throw new IllegalArgumentException("Scope '%s' not allowed for ref '%s'".formatted(scope, ref));
        }

        throw new IllegalStateException("Signer ref '%s' is address-only and cannot sign scope '%s'".formatted(ref, scope));
    }

    @Override
    public Optional<Wallet> asWallet() {
        return Optional.empty();
    }

    @Override
    public Optional<String> preferredAddress() {
        return Optional.ofNullable(address);
    }

    private String normalize(String scope) {
        if (scope == null) {
            return "";
        }

        return scope.trim().toLowerCase(Locale.ROOT);
    }
}

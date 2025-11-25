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
 * Scoped binding that rejects scopes not present in the allowed set.
 */
@RequiredArgsConstructor
public class ScopedSignerBinding implements SignerBinding {
    private final String ref;
    private final SignerBinding delegate;
    private final Set<String> allowedScopes;

    @Override
    public TxSigner signerFor(String scope) {
        String normalized = normalize(scope);
        if (!CollectionUtils.isEmpty(allowedScopes) && !allowedScopes.contains(normalized)) {
            throw new IllegalArgumentException("Scope '%s' not allowed for ref '%s'".formatted(scope, ref));
        }

        return delegate.signerFor(scope);
    }

    @Override
    public Optional<Wallet> asWallet() {
        return delegate.asWallet();
    }

    @Override
    public Optional<String> preferredAddress() {
        return delegate.preferredAddress();
    }

    private String normalize(String scope) {
        if (scope == null) {
            return "";
        }

        return scope.trim().toLowerCase(Locale.ROOT);
    }
}

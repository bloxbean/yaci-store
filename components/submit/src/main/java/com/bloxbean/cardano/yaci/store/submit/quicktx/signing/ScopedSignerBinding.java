package com.bloxbean.cardano.yaci.store.submit.quicktx.signing;

import com.bloxbean.cardano.client.function.TxSigner;
import com.bloxbean.cardano.client.quicktx.signing.SignerBinding;
import com.bloxbean.cardano.hdwallet.Wallet;
import org.springframework.util.CollectionUtils;

import java.util.Optional;
import java.util.Set;

/**
 * Scoped binding that rejects scopes not present in the allowed set.
 */
public class ScopedSignerBinding implements SignerBinding {
    private final String ref;
    private final SignerBinding delegate;
    private final Set<String> allowedScopes;

    public ScopedSignerBinding(String ref, SignerBinding delegate, Set<String> allowedScopes) {
        this.ref = ref;
        this.delegate = delegate;
        this.allowedScopes = allowedScopes;
    }

    @Override
    public TxSigner signerFor(String scope) {
        // Support comma-separated scopes like "payment,stake"
        // Split and check if ANY of the scopes is allowed
        String[] scopeParts = scope.split(",");
        for (String part : scopeParts) {
            String normalized = SignerScopeUtil.normalize(part);
            if (!CollectionUtils.isEmpty(allowedScopes) && allowedScopes.contains(normalized)) {
                // At least one scope matches - delegate to the underlying signer with the original scope
                return delegate.signerFor(scope);
            }
        }

        // None of the scopes matched
        if (!CollectionUtils.isEmpty(allowedScopes)) {
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
}

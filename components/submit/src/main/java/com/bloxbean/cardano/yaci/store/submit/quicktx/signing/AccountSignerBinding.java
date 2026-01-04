package com.bloxbean.cardano.yaci.store.submit.quicktx.signing;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.function.TxSigner;
import com.bloxbean.cardano.client.function.helper.SignerProviders;
import com.bloxbean.cardano.client.quicktx.signing.SignerBinding;
import com.bloxbean.cardano.hdwallet.Wallet;

import java.util.Optional;

/**
 * Signer binding backed by a local Account.
 */
public class AccountSignerBinding implements SignerBinding {
    private final Account account;

    public AccountSignerBinding(Account account) {
        this.account = account;
    }

    @Override
    public TxSigner signerFor(String scope) {
        String normalized = SignerScopeUtil.normalize(scope);

        return switch (normalized) {
            case "payment" -> SignerProviders.signerFrom(account);
            case "stake" -> SignerProviders.stakeKeySignerFrom(account);
            case "drep" -> SignerProviders.drepKeySignerFrom(account);
            case "committeecold" -> SignerProviders.committeeColdKeySignerFrom(account);
            case "committeehot" -> SignerProviders.committeeHotKeySignerFrom(account);
            case "policy" -> SignerProviders.signerFrom(account); // reuse payment key for policy if provided
            default -> throw new IllegalArgumentException("Unsupported scope '%s' for account signer".formatted(scope));
        };
    }

    @Override
    public Optional<Wallet> asWallet() {
        return Optional.empty();
    }

    @Override
    public Optional<String> preferredAddress() {
        Address address = account.getBaseAddress();
        return address == null ? Optional.empty() : Optional.ofNullable(address.toBech32());
    }
}

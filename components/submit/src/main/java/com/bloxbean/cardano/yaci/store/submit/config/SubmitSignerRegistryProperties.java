package com.bloxbean.cardano.yaci.store.submit.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ConfigurationProperties(prefix = "store.submit.signer-registry")
public class SubmitSignerRegistryProperties {
    private boolean enabled;

    @Builder.Default
    private List<Entry> entries = new ArrayList<>();

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Entry {
        private String ref;
        private SignerType type;

        @Builder.Default
        private Set<String> scopes = new HashSet<>();

        private AccountProperties account;
        private AddressOnlyProperties address;
        private RemoteSignerProperties remote;
    }

    public enum SignerType {
        ACCOUNT,
        ADDRESS_ONLY,
        REMOTE_SIGNER
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AccountProperties {
        /**
         * Mnemonic for the local account/wallet.
         */
        private String mnemonic;

        /**
         * Optional bech32 / hex private key material for account creation.
         */
        private String bech32PrivateKey;
        private String rootKeyHex;

        /**
         * Derivation indexes when using mnemonic / root keys.
         */
        @Builder.Default
        private int account = 0;

        @Builder.Default
        private int index = 0;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AddressOnlyProperties {
        private String address;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RemoteSignerProperties {
        private String endpoint;
        private String authToken;
        private String keyId;
        private String hostPublicKey;
        private String verificationKey;
        private String address;
        private Integer timeoutMs;
    }
}

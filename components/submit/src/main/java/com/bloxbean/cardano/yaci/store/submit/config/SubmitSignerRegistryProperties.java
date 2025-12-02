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
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "store.submit.signer-registry")
public class SubmitSignerRegistryProperties {

    private boolean enabled;

    @Builder.Default
    private List<Entry> entries = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
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
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountProperties {
        private String mnemonic;
        private String bech32PrivateKey;
        private String rootKeyHex;

        @Builder.Default
        private int account = 0;

        @Builder.Default
        private int index = 0;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressOnlyProperties {
        private String address;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RemoteSignerProperties {
        private String endpoint;
        private String authToken;
        private String keyId;
        private String verificationKey;
        private String address;
        private Integer timeoutMs;
    }
}

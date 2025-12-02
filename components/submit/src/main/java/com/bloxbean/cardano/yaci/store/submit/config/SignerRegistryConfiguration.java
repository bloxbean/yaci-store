package com.bloxbean.cardano.yaci.store.submit.config;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.common.model.Network;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.quicktx.signing.DefaultSignerRegistry;
import com.bloxbean.cardano.client.quicktx.signing.SignerBinding;
import com.bloxbean.cardano.client.quicktx.signing.SignerRegistry;
import com.bloxbean.cardano.client.quicktx.signing.SignerScopes;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.store.submit.config.SubmitSignerRegistryProperties.AccountProperties;
import com.bloxbean.cardano.yaci.store.submit.config.SubmitSignerRegistryProperties.AddressOnlyProperties;
import com.bloxbean.cardano.yaci.store.submit.config.SubmitSignerRegistryProperties.Entry;
import com.bloxbean.cardano.yaci.store.submit.config.SubmitSignerRegistryProperties.RemoteSignerProperties;
import com.bloxbean.cardano.yaci.store.submit.config.SubmitSignerRegistryProperties.SignerType;
import com.bloxbean.cardano.yaci.store.submit.signing.AccountSignerBinding;
import com.bloxbean.cardano.yaci.store.submit.signing.AddressOnlySignerBinding;
import com.bloxbean.cardano.yaci.store.submit.signing.RemoteSignerBinding;
import com.bloxbean.cardano.yaci.store.submit.signing.ScopedSignerBinding;
import com.bloxbean.cardano.yaci.store.submit.signing.remote.RemoteSignerClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Binds signer-registry configuration into a QuickTx SignerRegistry.
 */
@Configuration
@EnableConfigurationProperties(SubmitSignerRegistryProperties.class)
@ConditionalOnProperty(prefix = "store.submit.signer-registry", name = "enabled", havingValue = "true")
@Slf4j
public class SignerRegistryConfiguration {

    @Bean
    public SignerRegistry submitSignerRegistry(SubmitSignerRegistryProperties properties,
                                               Environment environment,
                                               Optional<RemoteSignerClient> remoteSignerClientOpt) {

        var registry = new DefaultSignerRegistry();
        Network network = resolveNetwork(environment);

        for (Entry entry : properties.getEntries()) {
            if (entry == null) continue;

            String ref = entry.getRef();
            if (!StringUtils.hasText(ref)) {
                log.warn("Ignoring signer-registry entry without ref");
                continue;
            }

            SignerType type = Optional.ofNullable(entry.getType()).orElse(SignerType.ACCOUNT);
            Set<String> scopes = normalizeScopes(entry.getScopes());

            try {
                switch (type) {
                    case ACCOUNT -> {
                        AccountProperties accountProps = entry.getAccount();
                        Account account = buildAccount(ref, accountProps, network);
                        if (account == null) {
                            continue;
                        }
                        SignerBinding binding = new AccountSignerBinding(account);
                        binding = new ScopedSignerBinding(ref, binding, scopes);
                        registry.addCustom(ref, binding);
                        log.info("Registered account signer ref={} scopes={}", ref, scopes);
                    }
                    case ADDRESS_ONLY -> {
                        AddressOnlyProperties addressProps = entry.getAddress();
                        if (addressProps == null || !StringUtils.hasText(addressProps.getAddress())) {
                            log.warn("Address-only signer ref={} ignored: address missing", ref);
                            continue;
                        }
                        var binding = new AddressOnlySignerBinding(ref, addressProps.getAddress(), scopes);
                        registry.addCustom(ref, binding);
                        log.info("Registered address-only signer ref={} scopes={}", ref, scopes);
                    }
                    case REMOTE_SIGNER -> {
                        if (remoteSignerClientOpt.isEmpty()) {
                            throw new IllegalStateException("Remote signer ref=" + ref + " configured but no RemoteSignerClient bean found");
                        }
                        RemoteSignerProperties remoteProps = entry.getRemote();
                        if (remoteProps == null || !StringUtils.hasText(remoteProps.getKeyId()) || !StringUtils.hasText(remoteProps.getEndpoint())) {
                            log.warn("Remote signer ref={} ignored: endpoint or keyId missing", ref);
                            continue;
                        }

                        var binding = new RemoteSignerBinding(ref, scopes, remoteProps, remoteSignerClientOpt.get());
                        registry.addCustom(ref, binding);
                        log.info("Registered remote signer ref={} scopes={}", ref, scopes);
                    }
                    default -> log.warn("Unsupported signer type {} for ref {}", type, ref);
                }
            } catch (RuntimeException e) {
                log.error("Failed to register signer ref={}: {}", ref, e.getMessage(), e);
            }
        }

        return registry;
    }

    private Network resolveNetwork(Environment environment) {
        long protocolMagic = environment.getProperty("store.cardano.protocol-magic", Long.class, Networks.preprod().getProtocolMagic());

        if (protocolMagic == Networks.mainnet().getProtocolMagic()) {
            return Networks.mainnet();
        } else if (protocolMagic == Networks.preprod().getProtocolMagic()) {
            return Networks.preprod();
        } else if (protocolMagic == Networks.preview().getProtocolMagic()) {
            return Networks.preview();
        }

        int networkId = protocolMagic == Networks.mainnet().getProtocolMagic() ? 1 : 0;
        return new Network(networkId, protocolMagic);
    }

    private Set<String> normalizeScopes(Set<String> scopes) {
        if (CollectionUtils.isEmpty(scopes)) {
            return Set.of(SignerScopes.PAYMENT.toLowerCase(Locale.ROOT));
        }

        return scopes.stream()
                .filter(StringUtils::hasText)
                .map(scope -> scope.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(HashSet::new));
    }

    private Account buildAccount(String ref, AccountProperties account, Network network) {
        if (account == null) {
            log.warn("Signer ref={} ignored: account details missing", ref);
            return null;
        }

        try {
            if (StringUtils.hasText(account.getMnemonic())) {
                return Account.createFromMnemonic(network, account.getMnemonic(), account.getAccount(), account.getIndex());
            }

            if (StringUtils.hasText(account.getRootKeyHex())) {
                byte[] rootKey = HexUtil.decodeHexString(account.getRootKeyHex());
                return Account.createFromRootKey(network, rootKey, account.getAccount(), account.getIndex());
            }

            if (StringUtils.hasText(account.getBech32PrivateKey())) {
                return new Account(network, account.getBech32PrivateKey(), account.getIndex());
            }
        } catch (Exception e) {
            log.error("Failed to build account signer ref={}: {}", ref, e.getMessage(), e);
            return null;
        }

        log.warn("Signer ref={} ignored: no key material provided", ref);
        return null;
    }
}

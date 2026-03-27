package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
public class Cip113Configuration {

    @Setter
    @Value("${store.extensions.asset-store.cip113.registry-nft-policy-ids:}")
    private List<String> registryNftPolicyIds;

    @Getter
    private Set<String> registryNftPolicyIdSet;

    @PostConstruct
    public void init() {
        registryNftPolicyIdSet = registryNftPolicyIds.stream()
                .filter(id -> !id.isBlank())
                .collect(Collectors.toUnmodifiableSet());
        log.info("INIT - CIP-113 programmable tokens: enabled={}, registryNftPolicyIds={}",
                isEnabled(), registryNftPolicyIdSet);
    }

    public boolean isEnabled() {
        return !registryNftPolicyIdSet.isEmpty();
    }

    public boolean isMonitoredPolicyId(String policyId) {
        return registryNftPolicyIdSet.contains(policyId);
    }

}

package com.bloxbean.cardano.yaci.store.api.staking.dto;

import com.bloxbean.cardano.client.address.AddressProvider;
import com.bloxbean.cardano.client.address.Credential;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.yaci.core.model.Relay;
import com.bloxbean.cardano.yaci.core.util.HexUtil;
import com.bloxbean.cardano.yaci.store.common.util.PoolUtil;
import com.bloxbean.cardano.yaci.store.staking.domain.PoolDetails;
import com.bloxbean.cardano.yaci.store.staking.domain.PoolStatusType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Slf4j
public record PoolDetailsDto(
        Integer epoch,
        String poolId,
        String poolHash,
        String vrfKeyHash,
        String pledge,
        String cost,
        String margin,
        String rewardAccount,
        Set<String> poolOwners,
        List<Relay> relays,

        String metadataUrl,
        String metadataHash,

        String txHash,
        Integer certIndex,
        PoolStatusType status,
        Integer retireEpoch
        ) {

    public static PoolDetailsDto toDto(PoolDetails details, boolean isMainnet) {
        String poolHash = details.getPoolId(); // PoolDetails's poolId is actually poolHash

        String bech32PoolId = null;
        if (poolHash != null) {
            bech32PoolId = PoolUtil.getBech32PoolId(poolHash);
        }

        var owners = details.getPoolOwners();
        if (details.getPoolOwners() != null) {
            try {
                owners = details.getPoolOwners()
                        .stream().map(ownerHash -> {
                            var credential = Credential.fromKey(HexUtil.decodeHexString(ownerHash));
                            return AddressProvider.getRewardAddress(credential, isMainnet? Networks.mainnet(): Networks.testnet()).toBech32();
                        })
                        .collect(Collectors.toSet());
            } catch (Exception e) {
                //ignore
                log.error("Pool owner address conversion error", e);
            }
        }

        return new PoolDetailsDto(
                details.getEpoch(),
                bech32PoolId,
                poolHash,
                details.getVrfKeyHash(),
                details.getPledge() != null? details.getPledge().toString() : null,
                details.getCost() != null? details.getCost().toString(): null,
                details.getMargin() != null? details.getMargin().toString(): null,
                details.getRewardAccount(),
                owners,
                details.getRelays(),
                details.getMetadataUrl(),
                details.getMetadataHash(),
                details.getTxHash(),
                details.getCertIndex(),
                details.getStatus(),
                details.getRetireEpoch()
        );
    }

}

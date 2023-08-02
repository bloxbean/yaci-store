package com.bloxbean.cardano.yaci.store.account.service;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.crypto.Bech32;
import com.bloxbean.cardano.yaci.core.protocol.localstate.queries.DelegationsAndRewardAccountsQuery;
import com.bloxbean.cardano.yaci.core.protocol.localstate.queries.DelegationsAndRewardAccountsResult;
import com.bloxbean.cardano.yaci.core.util.HexUtil;
import com.bloxbean.cardano.yaci.helper.LocalClientProvider;
import com.bloxbean.cardano.yaci.helper.LocalStateQueryClient;
import com.bloxbean.cardano.yaci.store.account.domain.StakeAccountRewardInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigInteger;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
@ConditionalOnExpression("'${store.cardano.n2c-node-socket-path:}' != '' || '${store.cardano.n2c-host:}' != ''")
public class AccountService {
    private final LocalStateQueryClient localStateQueryClient;

    public AccountService(LocalClientProvider localClientProvider) {
        if (localClientProvider != null) {
            this.localStateQueryClient = localClientProvider.getLocalStateQueryClient();
            log.info("Account Controller initialized >>>");
        } else
            localStateQueryClient = null;
    }

    /**
     * Get stake account info
     *
     * @param stakeAddress
     * @return StakeAccountInfo
     */
    public Optional<StakeAccountRewardInfo> getAccountInfo(String stakeAddress) {
        if (localStateQueryClient == null) {
            log.info("LocalStateQueryClient is not initialized. Please check if n2c-node-socket-path or n2c-host is configured properly.");
            return Optional.empty();
        }

        Address address = new Address(stakeAddress);
        DelegationsAndRewardAccountsResult delegationsAndRewardAccountsResult = null;
        //TODO -- This is a temporary impelementation. It is not scalable with synchronized block
        synchronized (this) {
            //Try to release first before a new query to avoid stale data
            try {
                localStateQueryClient.release().block(Duration.ofSeconds(5));
            } catch (Exception e) {
                //Ignore the error
            }

            Mono<DelegationsAndRewardAccountsResult> mono =
                    localStateQueryClient.executeQuery(new DelegationsAndRewardAccountsQuery(Set.of(address)));
            delegationsAndRewardAccountsResult = mono.block(Duration.ofSeconds(5));
        }

        if (delegationsAndRewardAccountsResult == null) {
            return Optional.empty();
        }

        String poolId = delegationsAndRewardAccountsResult.getDelegations() != null ?
                delegationsAndRewardAccountsResult.getDelegations().getOrDefault(address, null) : null;
        if (poolId != null)
            poolId = Bech32.encode(HexUtil.decodeHexString(poolId), "pool");

        BigInteger rewards = delegationsAndRewardAccountsResult.getRewards() != null ?
                delegationsAndRewardAccountsResult.getRewards().getOrDefault(address, BigInteger.ZERO) : BigInteger.ZERO;

        return Optional.of(new StakeAccountRewardInfo(stakeAddress, poolId, rewards));
    }
}

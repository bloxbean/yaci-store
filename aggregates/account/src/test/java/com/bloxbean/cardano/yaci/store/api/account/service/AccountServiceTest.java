package com.bloxbean.cardano.yaci.store.api.account.service;

import com.bloxbean.cardano.yaci.store.account.domain.StakeAccountRewardInfo;
import com.bloxbean.cardano.yaci.store.account.service.StakeAccountRewardProvider;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AccountServiceTest {
    private static final String STAKE_ADDRESS = "stake_test1up97ct2wt8jqlly2cnkhuwc7tvevmjpp7h6ts3rucpksy8c8cnspn";

    @Test
    void getAccountInfoReturnsEmptyWhenLocalStateAndFallbackProvidersAreUnavailable() {
        var accountService = new AccountService(null, null);

        var accountInfo = accountService.getAccountInfo(STAKE_ADDRESS);

        assertThat(accountInfo).isEmpty();
    }

    @Test
    void getAccountInfoUsesFallbackProviderWhenLocalStateIsUnavailable() {
        StakeAccountRewardProvider rewardProvider = stakeAddress -> Optional.of(StakeAccountRewardInfo.builder()
                .stakeAddress(stakeAddress)
                .withdrawableAmount(BigInteger.valueOf(40))
                .build());
        var accountService = new AccountService(null, null, rewardProvider);

        var accountInfo = accountService.getAccountInfo(STAKE_ADDRESS);

        assertThat(accountInfo).isPresent();
        assertThat(accountInfo.orElseThrow().getWithdrawableAmount()).isEqualTo(BigInteger.valueOf(40));
    }
}

package com.bloxbean.cardano.yaci.store.staking.client;

import com.bloxbean.cardano.yaci.store.client.staking.StakingClient;
import com.bloxbean.cardano.yaci.store.staking.storage.StakingCertificateStorageReader;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("stakingClient")
@Primary
@RequiredArgsConstructor
public class StakingClientImpl implements StakingClient {
    private final StakingCertificateStorageReader stakingCertificateStorageReader;

    @Override
    public Optional<String> getStakeAddressFromPointer(long slot, int txIndex, int certIndex) {
        return stakingCertificateStorageReader.getRegistrationByPointer(slot, txIndex, certIndex)
                .map(stakeRegistrationDetail -> stakeRegistrationDetail.getAddress());
    }
}

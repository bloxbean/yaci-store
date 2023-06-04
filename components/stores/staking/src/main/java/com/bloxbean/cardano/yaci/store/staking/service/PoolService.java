package com.bloxbean.cardano.yaci.store.staking.service;

import com.bloxbean.cardano.yaci.store.staking.domain.PoolRegistration;
import com.bloxbean.cardano.yaci.store.staking.domain.PoolRetirement;
import com.bloxbean.cardano.yaci.store.staking.storage.PoolStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PoolService {
    private final PoolStorage poolStorage;

    public List<PoolRegistration> getPoolRegistrations(int page, int count) {
        return poolStorage.findPoolRegistrations(page, count);
    }

    public List<PoolRetirement> getPoolRetirements(int page, int count) {
        return poolStorage.findPoolRetirements(page, count);
    }

}

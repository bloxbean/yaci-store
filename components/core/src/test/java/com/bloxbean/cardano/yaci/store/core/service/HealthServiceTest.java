package com.bloxbean.cardano.yaci.store.core.service;

import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HealthServiceTest {
    private BlockFetchService blockFetchService;
    private HealthService healthService;

    @BeforeEach
    void setUp() {
        blockFetchService = mock(BlockFetchService.class);
        StoreProperties storeProperties = new StoreProperties();
        storeProperties.setBlockReceiveDelaySeconds(120);
        healthService = new HealthService(blockFetchService, storeProperties);
    }

    @Test
    void nothingStartedYetCountsAsReceiving() {
        assertThat(healthService.getHealthStatus().isReceivingBlocks()).isTrue();
    }

    @Test
    void recentBlockCountsAsReceiving() {
        when(blockFetchService.getLastReceivedBlockTime()).thenReturn(System.currentTimeMillis() - 10_000);

        assertThat(healthService.getHealthStatus().isReceivingBlocks()).isTrue();
    }

    @Test
    void oldBlockAndOldSessionIsAStall() {
        long now = System.currentTimeMillis();
        when(blockFetchService.getLastReceivedBlockTime()).thenReturn(now - 600_000);
        when(blockFetchService.getLastSessionStartTime()).thenReturn(now - 500_000);

        assertThat(healthService.getHealthStatus().isReceivingBlocks()).isFalse();
    }

    @Test
    void newSessionGetsTheFullThresholdEvenIfTheLastBlockIsOld() {
        long now = System.currentTimeMillis();
        when(blockFetchService.getLastReceivedBlockTime()).thenReturn(now - 600_000);
        when(blockFetchService.getLastSessionStartTime()).thenReturn(now - 10_000);

        var status = healthService.getHealthStatus();
        assertThat(status.isReceivingBlocks()).isTrue();
        // what is reported about the last block does not change
        assertThat(status.getTimeSinceLastBlock()).isGreaterThanOrEqualTo(600_000);
    }

    @Test
    void sessionThatNeverReceivesABlockIsAStallOnceTheThresholdPasses() {
        when(blockFetchService.getLastSessionStartTime()).thenReturn(System.currentTimeMillis() - 200_000);

        assertThat(healthService.getHealthStatus().isReceivingBlocks()).isFalse();
    }
}

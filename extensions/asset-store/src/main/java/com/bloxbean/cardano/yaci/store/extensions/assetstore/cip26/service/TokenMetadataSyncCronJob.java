package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@ConditionalOnProperty(name = "store.extensions.asset-store.cip26.sync-enabled", havingValue = "true")
public class TokenMetadataSyncCronJob implements Runnable {

    private final TokenMetadataSyncService tokenMetadataSyncService;

    public TokenMetadataSyncCronJob(TokenMetadataSyncService tokenMetadataSyncService) {
        this.tokenMetadataSyncService = tokenMetadataSyncService;
    }

    @Override
    @Scheduled(timeUnit = TimeUnit.MINUTES, initialDelay = 1L, fixedDelay = 60L)
    public void run() {
        log.info("about to start syncing");
        tokenMetadataSyncService.synchronizeDatabase();
        log.info("syncing completed");
    }

    @PostConstruct
    public void logInitMessage() {
        log.info("CIP-26 offchain metadata sync cronjob initialised");
    }

}

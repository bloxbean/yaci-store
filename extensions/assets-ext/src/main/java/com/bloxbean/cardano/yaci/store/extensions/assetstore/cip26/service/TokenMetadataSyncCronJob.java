package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.service;

import com.bloxbean.cardano.yaci.store.core.annotation.ReadOnly;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Scheduled cron job that periodically syncs CIP-26 offchain metadata from the GitHub token registry.
 * <p>
 * Gated with {@code @ReadOnly(false)} to prevent the {@code @Scheduled} timer from firing in
 * read-only mode ({@code store.read-only-mode=true}). Unlike on-chain processors that are
 * implicitly safe (no block events are published in read-only mode), this cron job runs on
 * a fixed timer and would otherwise attempt git clone/pull and database writes.
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "store.assets.ext.cip26.enabled", havingValue = "true", matchIfMissing = true)
@ReadOnly(false)
public class TokenMetadataSyncCronJob implements Runnable {

    private final TokenMetadataSyncService tokenMetadataSyncService;

    public TokenMetadataSyncCronJob(TokenMetadataSyncService tokenMetadataSyncService) {
        this.tokenMetadataSyncService = tokenMetadataSyncService;
    }

    @Override
    @Scheduled(timeUnit = TimeUnit.MINUTES, initialDelay = 1L,
            fixedDelayString = "${store.assets.ext.cip26.sync-interval-minutes:60}")
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

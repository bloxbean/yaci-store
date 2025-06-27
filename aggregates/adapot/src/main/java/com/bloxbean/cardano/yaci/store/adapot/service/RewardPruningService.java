package com.bloxbean.cardano.yaci.store.adapot.service;


import com.bloxbean.cardano.yaci.store.adapot.AdaPotProperties;
import com.bloxbean.cardano.yaci.store.adapot.job.storage.AdaPotJobStorage;
import com.bloxbean.cardano.yaci.store.common.aspect.EnableIf;
import com.bloxbean.cardano.yaci.store.common.service.CursorService;
import com.bloxbean.cardano.yaci.store.events.internal.PreAdaPotJobProcessingEvent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.bloxbean.cardano.yaci.store.adapot.AdaPotConfiguration.STORE_ADAPOT_REWARD_PRUNING_ENABLED;

@Component
@ConditionalOnProperty(
        value = "store.adapot.reward-pruning-enabled",
        havingValue = "true",
        matchIfMissing = false
)
@RequiredArgsConstructor
@EnableIf(STORE_ADAPOT_REWARD_PRUNING_ENABLED)
@Slf4j
public class RewardPruningService {
    private final AdaPotProperties adaPotProperties;
    private final RewardService rewardService;
    private final CursorService cursorService;
    private final AtomicBoolean isPruning = new AtomicBoolean(false);

    @PostConstruct
    public void init() {
        log.info("<< Reward Pruning Service Enabled >>");
    }

    @Scheduled(fixedRateString = "${store.adapot.reward-pruning-interval:86400}", timeUnit = TimeUnit.SECONDS, initialDelay = 500)
    public void handleScheduledPruning() {
        if (!adaPotProperties.isRewardPruningEnabled()) {
            return;
        }

        if (isPruning.get()) {
            log.debug("Reward pruning is already in progress. Skipping this run !!!");
            return;
        }
        Thread.startVirtualThread(this::pruneWithdrawnRewards);
    }

    @EventListener
    @Transactional
    public void handlePreAdaPotJobProcessingEvent(PreAdaPotJobProcessingEvent event) {
        if (isPruning.get()) {
            log.debug("Reward pruning is already in progress. Skipping this run !!!");
            return;
        }

        Thread.startVirtualThread(this::pruneWithdrawnRewards);
        log.info("Reward pruning triggered by pre adapot job processing event");
    }

    private void pruneWithdrawnRewards() {
        isPruning.set(true);

        try {
            cursorService.getCursor().ifPresent(cursor -> {
                log.info("Current cursor: {}", cursor.getBlock());

                long slot = cursor.getSlot() - adaPotProperties.getRewardPruningSafeSlots();

                if (slot > 0) {
                    log.info(">> Pruning withdrawn reward before slot: {}", slot);
                    long t1 = System.currentTimeMillis();
                    int deleteCount = rewardService.deleteWithdrawnRewards(slot);
                    long t2 = System.currentTimeMillis();

                    log.info(">> Deleted {} reward records before slot {}, Time taken: {} ms",
                            deleteCount, slot, (t2 - t1));
                }
            });
        } finally {
            isPruning.set(false);
        }
    }
}

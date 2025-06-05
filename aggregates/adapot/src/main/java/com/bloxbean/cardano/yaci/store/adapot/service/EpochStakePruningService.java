package com.bloxbean.cardano.yaci.store.adapot.service;

import com.bloxbean.cardano.yaci.store.adapot.AdaPotProperties;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJob;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJobStatus;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJobType;
import com.bloxbean.cardano.yaci.store.adapot.job.storage.AdaPotJobStorage;
import com.bloxbean.cardano.yaci.store.common.aspect.EnableIf;
import com.bloxbean.cardano.yaci.store.events.internal.PreAdaPotJobProcessingEvent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.bloxbean.cardano.yaci.store.adapot.AdaPotConfiguration.STORE_ADAPOT_EPOCH_STAKE_PRUNING_ENABLED;

@Component
@ConditionalOnProperty(
        value = "store.adapot.epoch-stake-pruning-enabled",
        havingValue = "true",
        matchIfMissing = false
)
@RequiredArgsConstructor
@EnableIf(STORE_ADAPOT_EPOCH_STAKE_PRUNING_ENABLED)
@Slf4j
public class EpochStakePruningService {
    private final EpochStakeService epochStakeService;
    private final AdaPotProperties adaPotProperties;
    private final PlatformTransactionManager transactionManager;
    private final AdaPotJobStorage adaPotJobStorage;
    private TransactionTemplate transactionTemplate;
    private AtomicBoolean isPruning = new AtomicBoolean(false);

    @PostConstruct
    public void init() {
        log.info("<< Epoch Stake Pruning Service Enabled >>");
        transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Scheduled(fixedRateString = "${store.adapot.epoch-stake-pruning-interval:86400}", timeUnit = TimeUnit.SECONDS, initialDelay = 300)
    public void handleScheduledPruning() {
        if (!adaPotProperties.isEpochStakePruningEnabled()) {
            return;
        }

        if (isPruning.get()) {
            log.debug("Epoch stake pruning is already in progress. Skipping this run !!!");
            return;
        }

        Thread.startVirtualThread(this::pruneOldEpochStakes);
    }

    @EventListener
    @Transactional
    @Order(1)
    public void handlePreAdaPotJobProcessingEvent(PreAdaPotJobProcessingEvent event) {
        if (isPruning.get()) {
            log.debug("Epoch stake pruning is already in progress. Skipping this run !!!");
            return;
        }

        Thread.startVirtualThread(this::pruneOldEpochStakes);
        log.info("Epoch stake pruning triggered by pre adapot job processing event");
    }

    private void pruneOldEpochStakes() {
        isPruning.set(true);
        try {
            List<AdaPotJob> completedJobs = adaPotJobStorage.getJobsByTypeAndStatus(AdaPotJobType.REWARD_CALC, AdaPotJobStatus.COMPLETED)
                    .stream()
                    .sorted(Comparator.comparingInt(AdaPotJob::getEpoch))
                    .toList();

            if (completedJobs.isEmpty()) {
                return;
            }

            int maxCompletedEpoch = completedJobs.getLast().getEpoch();

            int retentionEpochs = adaPotProperties.getEpochStakePruningSafeEpochs();

            if (retentionEpochs < 4) {
                log.error("Epoch stake safe epochs is set to less than 4." +
                        " This may cause issues with reward calculation. Minimum recommended value is 4. Current value: {}", retentionEpochs);
                return;
            }

            int pruneBeforeEpoch = maxCompletedEpoch - retentionEpochs + 1;

            if (pruneBeforeEpoch > 0) {
                log.info(">> Pruning epoch stake records before epoch: {}", pruneBeforeEpoch);

                long t1 = System.currentTimeMillis();
                long deleteCount = epochStakeService.deleteByEpochLessThan(pruneBeforeEpoch);
                long t2 = System.currentTimeMillis();

                log.info(">> Deleted {} epoch stake records before epoch {}, Time taken: {} ms",
                        deleteCount, pruneBeforeEpoch, (t2 - t1));
            } else {
                log.debug("No epoch stake records to prune. Max epoch: {}, Retention: {} epochs",
                        maxCompletedEpoch, retentionEpochs);
            }
        } finally {
            isPruning.set(false);
        }
    }

}

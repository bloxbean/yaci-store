package com.bloxbean.cardano.yaci.store.account.processor;

import com.bloxbean.cardano.yaci.store.account.AccountStoreProperties;
import com.bloxbean.cardano.yaci.store.account.service.AccountConfigService;
import com.bloxbean.cardano.yaci.store.account.storage.impl.jpa.model.AccountConfigEntity;
import com.bloxbean.cardano.yaci.store.account.util.ConfigIds;
import com.bloxbean.cardano.yaci.store.account.util.ConfigStatus;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "store.account.batch-balance-aggregation-scheduler-enabled", havingValue = "true", matchIfMissing = false)
@Slf4j
public class AccountBalanceSchedulerService {
    private final AccountBalanceBatchProcessingService accountBalanceBatchProcessingService;
    private final AccountConfigService accountConfigService;
    private final AccountStoreProperties accountStoreProperties;

    private boolean isStopped = false;

    @PostConstruct
    private void init() {
        log.info("<<< Account balance aggregation job is enabled >>>");
    }

    @Scheduled(fixedRateString = "#{accountStoreProperties.batchBalanceAggregationScheduleDelay * 1000}", initialDelay = 10000)
    public void handleReadyForAggregationEvent() {
        if (isStopped) return;

        var configEntity = accountConfigService.getConfig(ConfigIds.ACCOUNT_BALANCE_AGGR_JOB_ID);
        ConfigStatus currentStatus = configEntity.map(AccountConfigEntity::getStatus).orElse(null);
        var configBlock = configEntity.map(AccountConfigEntity::getBlock).orElse(null);

        //If request to stop due from main sync job, then stop this job
        if (ConfigStatus.BATCH_AGGR_REQUEST_TO_STOP == currentStatus) {
            log.info("Stopping account balance aggregation job >>>");
            accountConfigService.upateConfig(ConfigIds.ACCOUNT_BALANCE_AGGR_JOB_ID, ConfigStatus.BATCH_AGGR_STOPPED, configBlock);
            return;
        }

        //If job has already been stopped, then skip
        if (ConfigStatus.BATCH_AGGR_STOPPED == currentStatus) {
            log.debug("Account balance aggregation job has already been stopped. Skipping ...");
            //Set stop flag to avoid unnecessary processing
            isStopped = true;
            return;
        }

        //If status is null, then set it to in progress
        if (currentStatus == null)
            accountConfigService.upateConfig(ConfigIds.ACCOUNT_BALANCE_AGGR_JOB_ID, ConfigStatus.BATCH_AGGR_IN_PROGRESS, null);

        Long maxBlockNumber = accountConfigService.getConfig(ConfigIds.LAST_PROCESSED_BLOCK).map(AccountConfigEntity::getBlock).orElse(null);

        if (maxBlockNumber == null) maxBlockNumber = 0L;

        //safe block diff is there to make sure we don't miss block due to rollback, so process till maxBlockNumber - safeBlockDiff
        maxBlockNumber = maxBlockNumber - accountStoreProperties.getBatchBalanceAggregationSafeBlockDiff();
        if (maxBlockNumber < 0) return;

        accountBalanceBatchProcessingService.runBalanceCalculationBatch(maxBlockNumber, accountStoreProperties.getBatchBalanceAggregationBatchSize());
    }

}

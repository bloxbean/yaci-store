package com.bloxbean.cardano.yaci.store.adapot.job;

import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJob;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJobStatus;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJobType;
import com.bloxbean.cardano.yaci.store.adapot.job.storage.AdaPotJobStorage;
import com.bloxbean.cardano.yaci.store.adapot.service.AdaPotService;
import com.bloxbean.cardano.yaci.store.adapot.service.EpochRewardCalculationService;
import com.bloxbean.cardano.yaci.store.adapot.snapshot.DepositSnapshotService;
import com.bloxbean.cardano.yaci.store.adapot.snapshot.StakeSnapshotService;
import com.bloxbean.cardano.yaci.store.adapot.snapshot.UtxoSnapshotService;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.executor.ParallelExecutor;
import com.bloxbean.cardano.yaci.store.core.annotation.ReadOnly;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

/**
 * Manages and processes AdaPot jobs, specifically related to reward calculations.
 * Initializes necessary services and properties upon instantiation, and starts job processing in a virtual thread.
 * It resets any jobs that were in the 'STARTED' state to 'NOT_STARTED' on a restart and loads any pending jobs.
 */
@Component
@ReadOnly(false)
@Slf4j
public class AdaPotJobManager {
    private final BlockingQueue<AdaPotJob> jobQueue = new LinkedBlockingQueue<>();
    private StoreProperties storeProperties;
    private AdaPotJobStorage adaPotJobStorage;
    private EraService eraService;
    private EpochRewardCalculationService epochRewardCalculationService;
    private StakeSnapshotService stakeSnapshotService;
    private DepositSnapshotService depositSnapshotService;
    private AdaPotService adaPotService;

    public AdaPotJobManager(StoreProperties storeProperties,
                            AdaPotJobStorage adaPotJobStorage,
                            EraService eraService,
                            EpochRewardCalculationService epochRewardCalculationService,
                            StakeSnapshotService stakeSnapshotService,
                            DepositSnapshotService depositSnapshotService,
                            UtxoSnapshotService utxoSnapshotService,
                            AdaPotService adaPotService,
                            ParallelExecutor parallelExecutor) {
        this.storeProperties = storeProperties;
        this.adaPotJobStorage = adaPotJobStorage;
        this.eraService = eraService;
        this.epochRewardCalculationService = epochRewardCalculationService;
        this.stakeSnapshotService = stakeSnapshotService;
        this.depositSnapshotService = depositSnapshotService;
        this.adaPotService = adaPotService;

        // Reset jobs that were in 'STARTED' state to 'NOT_STARTED' and load pending jobs
        resetStartedJobs();
        loadPendingJobs();

        // Start a virtual thread for job processing
        Thread.startVirtualThread(this::processJobs);
    }

    // Reset jobs from STARTED to NOT_STARTED on restart
    private void resetStartedJobs() {
        List<AdaPotJob> startedJobs = adaPotJobStorage.getJobsByTypeAndStatus(AdaPotJobType.REWARD_CALC, AdaPotJobStatus.STARTED);
        startedJobs.forEach(job -> {
            job.setStatus(AdaPotJobStatus.NOT_STARTED);
            adaPotJobStorage.save(job);
        });
    }

    private void loadPendingJobs() {
        List<AdaPotJob> pendingJobs = adaPotJobStorage.getJobsByTypeAndStatus(AdaPotJobType.REWARD_CALC, AdaPotJobStatus.NOT_STARTED);
        jobQueue.addAll(pendingJobs);
    }

    /**
     * Schedules a reward calculation job by creating a new AdaPotJob instance
     * and adding it to the job queue.
     *
     * @param epoch the epoch number for which the reward calculation job is to be triggered
     * @param slot slot number
     */
    public void triggerRewardCalcJob(int epoch, long slot) {
        AdaPotJob job = new AdaPotJob(epoch, slot, AdaPotJobType.REWARD_CALC, AdaPotJobStatus.NOT_STARTED, 0L, 0L, 0L, 0L, null);
        adaPotJobStorage.save(job);
        jobQueue.add(job);
    }

    private void processJobs() {
        while (true) {
            try {
                AdaPotJob job = jobQueue.take();
                log.info("Found reward calc job in queue : {}", job);

                if (job == null)
                    continue;

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (job != null) {
                    boolean status = processJob(job);
                    if (!status) {
                        log.error("Reward calculation failed for epoch : " + job.getEpoch());
                        return;
                    }
                }
            } catch (Exception e) {
                log.error("Error processing reward calc job", e);
            }
        }
    }

    private boolean processJob(AdaPotJob job) throws InterruptedException {
        // Set job status to STARTED and update in the database
        job.setStatus(AdaPotJobStatus.STARTED);
        adaPotJobStorage.save(job);

        int retryCount = 0;

        while (true) {
            // Reset times
            job.setTotalTime(0L);
            job.setRewardCalcTime(0L);
            job.setUpdateRewardTime(0L);

            var start = Instant.now();
            var deposits = depositSnapshotService.getNetStakeDepositInEpoch(job.getEpoch() - 1);
            adaPotService.updateAdaPotDeposit(job.getEpoch(), deposits);
            var end = Instant.now();
            log.info("Deposit snapshot time in millis : {}, epoch: {}", end.toEpochMilli() - start.toEpochMilli(), job.getEpoch());

            start = Instant.now();
            boolean success = calculateRewards(job);
            end = Instant.now();
            job.setTotalTime(end.toEpochMilli() - start.toEpochMilli());
            log.info("Reward calculation time in millis : {}, epoch: {}", end.toEpochMilli() - start.toEpochMilli(), job.getEpoch());

            /**
             //Take UTXO snapshot
             var utxoSnapshotFuture = CompletableFuture.supplyAsync(() -> {
             var start = Instant.now();
             var utxo = utxoSnapshotService.getTotalUtxosInEpoch(job.getEpoch(), job.getSlot());
             adaPotService.updateEpochUtxo(job.getEpoch(), utxo);
             var end = Instant.now();
             log.info("UTXO snapshot time in millis : {}, epoch: {}", end.toEpochMilli() - start.toEpochMilli(), job.getEpoch());
             return true;
             }, parallelExecutor.getVirtualThreadExecutor());
             **/


            if (success) {
                job.setStatus(AdaPotJobStatus.COMPLETED);
                job.setErrorMessage(null);
                adaPotJobStorage.save(job);
                return true;
            } else {
                job.setErrorMessage("Reward calculation failed");
                adaPotJobStorage.save(job);
                //TODO -- Retry logic
                log.error("Reward calculation failed for epoch " + job.getEpoch() + ", retrying...");
                retryCount++;

                if (retryCount > 3) {
                    log.error("Reward calculation failed for epoch " + job.getEpoch() + ", retry count exceeded. Marking as failed");
                    job.setErrorMessage("Reward calculation failed. Retry count exceeded");
                    adaPotJobStorage.save(job);
                    return false;
                }

                Thread.sleep(5000);
            }
        }
    }

    private boolean calculateRewards(AdaPotJob job) {
        try {
            long nonByronEpoch = eraService.getFirstNonByronEpoch().orElse(0);

            if (job.getEpoch() < nonByronEpoch) {
                log.info("Epoch : {} is Byron era. Skipping reward calculation", job.getEpoch());
                return true;
            }

            //Calculate epoch rewards
            var start = Instant.now();
            int epoch = job.getEpoch();
            var epochCalculationResult = epochRewardCalculationService.calculateEpochRewards(epoch);
            var end = Instant.now();
            job.setRewardCalcTime(end.toEpochMilli() - start.toEpochMilli());

            if (storeProperties.isMainnet()) {
                //TODO -- Verify treasury and rewards value
                try {
                    var expectedPots = loadExpectedAdaPotValues();
                    var expectedPot = expectedPots.get(epoch);

                    if (expectedPot != null) {
                        if (!epochCalculationResult.getReserves().equals(expectedPot.getReserves()))
                            throw new RuntimeException("Reserves value mismatch for epoch : " + epoch);
                        if (!epochCalculationResult.getTreasury().equals(expectedPot.getTreasury()))
                            throw new RuntimeException("Treasury value mismatch for epoch : " + epoch);

                        log.info("Treasury and reserves value matched for epoch : {}", epoch);
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Match with expected adapot failed ", e);
                }
            }

            //update rewards
            start = Instant.now();
            epochRewardCalculationService.updateEpochRewards(epoch, epochCalculationResult);
            end = Instant.now();
            job.setUpdateRewardTime(end.toEpochMilli() - start.toEpochMilli());

            //Now take snapshot
            start = Instant.now();
            stakeSnapshotService.takeStakeSnapshot(epoch - 1);
            end = Instant.now();
            job.setStakeSnapshotTime(end.toEpochMilli() - start.toEpochMilli());

            return true;
        } catch (Exception e) {
            log.error("Error calculating rewards for epoch : " + job.getEpoch(), e);
            return false;
        }
    }

    public List<AdaPotJob> getPendingJobs() {
        return adaPotJobStorage.getJobsByTypeAndStatus(AdaPotJobType.REWARD_CALC, AdaPotJobStatus.NOT_STARTED);
    }

    public List<AdaPotJob> getCompletedJobs() {
        return adaPotJobStorage.getJobsByTypeAndStatus(AdaPotJobType.REWARD_CALC, AdaPotJobStatus.COMPLETED);
    }

    private Map<Integer, ExpectedAdaPot> loadExpectedAdaPotValues() throws IOException {
        String file = "dbsync_ada_pots.json";
        ObjectMapper objectMapper = new ObjectMapper();
        List<ExpectedAdaPot> pots = objectMapper.readValue(this.getClass().getClassLoader().getResourceAsStream(file), new TypeReference<List<ExpectedAdaPot>>() {
        });

        Map<Integer, ExpectedAdaPot> potsMap = pots.stream()
                .collect(Collectors.toMap(ExpectedAdaPot::getEpochNo, pot -> pot));

        return potsMap;
    }

}

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
class ExpectedAdaPot {
    private int epochNo;
    private BigInteger treasury;
    private BigInteger reserves;
    private BigInteger fees;
    private BigInteger deposits;
    private BigInteger utxo;
}


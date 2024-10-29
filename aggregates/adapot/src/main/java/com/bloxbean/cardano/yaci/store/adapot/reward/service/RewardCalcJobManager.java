package com.bloxbean.cardano.yaci.store.adapot.reward.service;

import com.bloxbean.cardano.yaci.store.adapot.reward.domain.RewardCalcJob;
import com.bloxbean.cardano.yaci.store.adapot.reward.domain.RewardCalcStatus;
import com.bloxbean.cardano.yaci.store.adapot.reward.storage.RewardCalcJobStorage;
import com.bloxbean.cardano.yaci.store.adapot.service.EpochRewardCalculationService;
import com.bloxbean.cardano.yaci.store.adapot.snapshot.StakeSnapshotService;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
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

@Component
@Slf4j
public class RewardCalcJobManager {
    private final BlockingQueue<RewardCalcJob> jobQueue = new LinkedBlockingQueue<>();
    private StoreProperties storeProperties;
    private RewardCalcJobStorage rewardCalcJobStorage;
    private EraService eraService;
    private EpochRewardCalculationService epochRewardCalculationService;
    private StakeSnapshotService stakeSnapshotService;

    public RewardCalcJobManager(StoreProperties storeProperties,
                                RewardCalcJobStorage rewardCalcJobStorage,
                                EraService eraService,
                                EpochRewardCalculationService epochRewardCalculationService,
                                StakeSnapshotService stakeSnapshotService) {
        this.storeProperties = storeProperties;
        this.rewardCalcJobStorage = rewardCalcJobStorage;
        this.eraService = eraService;
        this.epochRewardCalculationService = epochRewardCalculationService;
        this.stakeSnapshotService = stakeSnapshotService;

        // Reset jobs that were in 'STARTED' state to 'NOT_STARTED' and load pending jobs
        resetStartedJobs();
        loadPendingJobs();

        // Start a virtual thread for job processing
        Thread.startVirtualThread(this::processJobs);
    }

    // Reset jobs from STARTED to NOT_STARTED on restart
    private void resetStartedJobs() {
        List<RewardCalcJob> startedJobs = rewardCalcJobStorage.getJobsByStatus(RewardCalcStatus.STARTED);
        startedJobs.forEach(job -> {
            job.setStatus(RewardCalcStatus.NOT_STARTED);
            rewardCalcJobStorage.save(job);
        });
    }

    private void loadPendingJobs() {
        List<RewardCalcJob> pendingJobs = rewardCalcJobStorage.getJobsByStatus(RewardCalcStatus.NOT_STARTED);
        jobQueue.addAll(pendingJobs);
    }

    public void triggerRewardCalcJob(int epoch, long slot) {
        RewardCalcJob job = new RewardCalcJob(epoch, slot, RewardCalcStatus.NOT_STARTED, 0L, 0L, 0L, 0L, null);
        rewardCalcJobStorage.save(job);
        jobQueue.add(job);
    }

    private void processJobs() {
        while (true) {
            try {
                RewardCalcJob job = jobQueue.take();
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

    private boolean processJob(RewardCalcJob job) throws InterruptedException {
        // Set job status to STARTED and update in the database
        job.setStatus(RewardCalcStatus.STARTED);
        rewardCalcJobStorage.save(job);

        int retryCount = 0;

        while (true) {

            // Reset times
            job.setTotalTime(0L);
            job.setRewardCalcTime(0L);
            job.setUpdateRewardTime(0L);

            var start = Instant.now();

            //calculate rewards
            boolean success = calculateRewards(job);

            var end = Instant.now();
            job.setTotalTime(end.toEpochMilli() - start.toEpochMilli());

            if (success) {
                job.setStatus(RewardCalcStatus.COMPLETED);
                job.setErrorMessage(null);
                rewardCalcJobStorage.save(job);
                return true;
            } else {
                job.setErrorMessage("Reward calculation failed");
                rewardCalcJobStorage.save(job);
                //TODO -- Retry logic
                log.error("Reward calculation failed for epoch " + job.getEpoch() + ", retrying...");
                retryCount++;

                if(retryCount > 3) {
                    log.error("Reward calculation failed for epoch " + job.getEpoch() + ", retry count exceeded. Marking as failed");
                    job.setErrorMessage("Reward calculation failed. Retry count exceeded");
                    rewardCalcJobStorage.save(job);
                    return false;
                }

                Thread.sleep(5000);
            }
        }
    }

    private boolean calculateRewards(RewardCalcJob job) {
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

    public List<RewardCalcJob> getPendingJobs() {
        return rewardCalcJobStorage.getJobsByStatus(RewardCalcStatus.NOT_STARTED);
    }

    public List<RewardCalcJob> getCompletedJobs() {
        return rewardCalcJobStorage.getJobsByStatus(RewardCalcStatus.COMPLETED);
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


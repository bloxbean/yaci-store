package com.bloxbean.cardano.yaci.store.adapot.job;

import com.bloxbean.cardano.yaci.store.adapot.AdaPotProperties;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJob;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJobStatus;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJobType;
import com.bloxbean.cardano.yaci.store.adapot.job.storage.AdaPotJobStorage;
import com.bloxbean.cardano.yaci.store.adapot.service.AdaPotService;
import com.bloxbean.cardano.yaci.store.adapot.service.EpochRewardCalculationService;
import com.bloxbean.cardano.yaci.store.adapot.snapshot.DepositSnapshotService;
import com.bloxbean.cardano.yaci.store.adapot.snapshot.StakeSnapshotService;
import com.bloxbean.cardano.yaci.store.adapot.storage.PartitionManager;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import com.bloxbean.cardano.yaci.store.core.annotation.ReadOnly;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import com.bloxbean.cardano.yaci.store.events.domain.StakeSnapshotTakenEvent;
import com.bloxbean.cardano.yaci.store.events.internal.PreAdaPotJobProcessingEvent;
import com.bloxbean.cardano.yaci.store.transaction.storage.TransactionStorageReader;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.vavr.control.Either;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@ReadOnly(false)
@RequiredArgsConstructor
@Slf4j
public class AdaPotJobProcessor {
    private final StoreProperties storeProperties;
    private final AdaPotProperties adaPotProperties;
    private final AdaPotJobStorage adaPotJobStorage;
    private final EraService eraService;
    private final EpochRewardCalculationService epochRewardCalculationService;
    private final StakeSnapshotService stakeSnapshotService;
    private final DepositSnapshotService depositSnapshotService;
    private final AdaPotService adaPotService;
    private final TransactionStorageReader transactionStorageReader;
    private final ApplicationEventPublisher publisher;
    private final PartitionManager partitionManager;
    private final DSLContext dsl;

    public boolean processJob(AdaPotJob job) throws InterruptedException {
        // Set job status to STARTED and update in the database
        job.setStatus(AdaPotJobStatus.STARTED);
        adaPotJobStorage.save(job);

        int retryCount = 0;

        try {
            while (true) {
                // Reset times
                job.setTotalTime(0L);
                job.setRewardCalcTime(0L);
                job.setUpdateRewardTime(0L);

                var start = Instant.now();

                //Fire pre-adapot job processing event
                var preAdaPotJobProcessingEvent = new PreAdaPotJobProcessingEvent(job.getEpoch(), job.getSlot(), job.getBlock());
                publisher.publishEvent(preAdaPotJobProcessingEvent);

                //create AdaPot entry for the epoch
                adaPotService.createAdaPot(job.getEpoch(), job.getSlot());

                //Update Fee pot
                var totalFeeInEpoch = transactionStorageReader.getTotalFee(job.getEpoch() - 1); //Prev epoch
                if (totalFeeInEpoch == null) totalFeeInEpoch = BigInteger.ZERO;
                log.info("Total fee in epoch {} : {}", job.getEpoch() - 1, totalFeeInEpoch);
                //Update total fee in the epoch
                adaPotService.updateEpochFee(job.getEpoch(), totalFeeInEpoch);
                var end = Instant.now();
                log.info("Fee snapshot time in millis : {}, epoch: {}", end.toEpochMilli() - start.toEpochMilli(), job.getEpoch());

                //Update deposit stake pot
                start = Instant.now();
                var deposits = depositSnapshotService.getNetStakeDepositInEpoch(job.getEpoch() - 1);
                adaPotService.updateAdaPotDeposit(job.getEpoch(), deposits);
                end = Instant.now();
                log.info("Deposit snapshot time in millis : {}, epoch: {}", end.toEpochMilli() - start.toEpochMilli(), job.getEpoch());

                //Calculate rewards
                start = Instant.now();
                Either<String, Boolean> result = calculateRewards(job);
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


                if (result.isRight()) {
                    job.setStatus(AdaPotJobStatus.COMPLETED);
                    job.setErrorMessage(null);
                    adaPotJobStorage.save(job);
                    return true;
                } else {
                    job.setErrorMessage("Reward calculation failed : " + result.getLeft());
                    adaPotJobStorage.save(job);
                    //TODO -- Retry logic
                    log.error("Reward calculation failed for epoch " + job.getEpoch() + ", retrying...");
                    retryCount++;

                    if (retryCount > 3) {
                        log.error("Reward calculation failed for epoch " + job.getEpoch() + ", retry count exceeded. Marking as failed");
                        job.setErrorMessage("Reward calculation failed. Retry count exceeded : " + result.getLeft());
                        adaPotJobStorage.save(job);
                        return false;
                    }

                    Thread.sleep(5000);
                }
            }
        } catch (Exception e) {
            log.error("Adapot job processing failed", e);
            job.setErrorMessage("Reward calculation failed due to unknown exception : " + e.getMessage());
            adaPotJobStorage.save(job);
            return false;
        }
    }

    private Either<String, Boolean> calculateRewards(AdaPotJob job) {
        try {
            long nonByronEpoch = eraService.getFirstNonByronEpoch().orElse(0);

            if (job.getEpoch() < nonByronEpoch) {
                log.info("Epoch : {} is Byron era. Skipping reward calculation", job.getEpoch());
                return Either.right(true);
            }

            //Calculate epoch rewards
            var start = Instant.now();
            int epoch = job.getEpoch();
            var epochCalculationResult = epochRewardCalculationService.calculateEpochRewards(epoch);
            var end = Instant.now();
            job.setRewardCalcTime(end.toEpochMilli() - start.toEpochMilli());

            if (adaPotProperties.isVerifyAdapotCalcValues() &&
                    (storeProperties.isMainnet()
                            || storeProperties.getProtocolMagic() == 1
                            || storeProperties.getProtocolMagic() == 2)
            ) { //mainnet or preprod or preview
                //TODO -- Verify treasury and rewards value
                try {
                    var expectedPots = loadExpectedAdaPotValues(storeProperties.getProtocolMagic());
                    var expectedPot = expectedPots.get(epoch);

                    if (expectedPot != null) {
                        if (!epochCalculationResult.getReserves().equals(expectedPot.getReserves())) {
                            log.error("Expected reserves: {}, Calculated reserves: {}",
                                    expectedPot.getReserves(), epochCalculationResult.getReserves());
                            throw new RuntimeException("Reserves value mismatch for epoch : " + epoch
                                    + ", Expected: " + expectedPot.getReserves() + ", Actual: " + epochCalculationResult.getReserves());
                        }

                        if (!epochCalculationResult.getTreasury().equals(expectedPot.getTreasury())) {
                            log.error("Expected treasury: {}, Calculated treasury: {}",
                                    expectedPot.getTreasury(), epochCalculationResult.getTreasury());
                            throw new RuntimeException("Treasury value mismatch for epoch : " + epoch
                                    + ", Expected: " + expectedPot.getTreasury() + ", Actual: " + epochCalculationResult.getTreasury());
                        }

                        log.info("Treasury and reserves value matched for epoch : {}", epoch);
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Match with expected adapot failed ", e);
                }
            }

            //update rewards
            start = Instant.now();
            // Ensure partition exists for PostgreSQL (other databases use regular tables)
            if (dsl.dialect().family() == SQLDialect.POSTGRES) {
                partitionManager.ensureRewardPartition(epoch);
            }
            epochRewardCalculationService.updateEpochRewards(epoch, epochCalculationResult);
            end = Instant.now();
            job.setUpdateRewardTime(end.toEpochMilli() - start.toEpochMilli());

            //Now take snapshot
            start = Instant.now();
            stakeSnapshotService.takeStakeSnapshot(epoch - 1);
            end = Instant.now();
            job.setStakeSnapshotTime(end.toEpochMilli() - start.toEpochMilli());

            publisher.publishEvent(new StakeSnapshotTakenEvent(epoch - 1, job.getSlot()));

            adaPotJobStorage.getJobByTypeAndEpoch(AdaPotJobType.REWARD_CALC, epoch)
                    .ifPresent(adaPotJob -> {
                        job.setDrepDistrSnapshotTime(adaPotJob.getDrepDistrSnapshotTime());
                        job.setExtraInfo(adaPotJob.getExtraInfo());
                    });

            return Either.right(true);
        } catch (Exception e) {
            log.error("Error calculating rewards for epoch : " + job.getEpoch(), e);
            return Either.left(e.getMessage());
        }
    }

    public List<AdaPotJob> getPendingJobs() {
        return adaPotJobStorage.getJobsByTypeAndStatus(AdaPotJobType.REWARD_CALC, AdaPotJobStatus.NOT_STARTED);
    }

    public List<AdaPotJob> getCompletedJobs() {
        return adaPotJobStorage.getJobsByTypeAndStatus(AdaPotJobType.REWARD_CALC, AdaPotJobStatus.COMPLETED);
    }

    private Map<Integer, ExpectedAdaPot> loadExpectedAdaPotValues(long protocolMagic) throws IOException {
        String file = "dbsync_ada_pots.json";
        if (protocolMagic == 1) { //preprod
            file = "dbsync_ada_pots_preprod.json";
        } else if (protocolMagic == 2) { //preview
            file = "dbsync_ada_pots_preview.json";
        }

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

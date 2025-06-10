package com.bloxbean.cardano.yaci.store.adapot.metrics;

import com.bloxbean.cardano.yaci.store.adapot.AdaPotProperties;
import com.bloxbean.cardano.yaci.store.adapot.domain.AdaPot;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJob;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJobStatus;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJobType;
import com.bloxbean.cardano.yaci.store.adapot.job.storage.AdaPotJobStorage;
import com.bloxbean.cardano.yaci.store.adapot.storage.AdaPotStorage;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class AdaPotMetricService {
    public static final String YACI_STORE_ADAPOT_JOB_LAST_SUCCESSFUL_EPOCH = "yaci.store.adapot.job.last.successful.epoch";
    public static final String YACI_STORE_ADAPOT_JOB_LAST_SUCCESSFUL_TOTAL_TIME = "yaci.store.adapot.job.last.successful.total_time";
    public static final String YACI_STORE_ADAPOT_JOB_LAST_SUCCESSFUL_REWARDCALC_TIME = "yaci.store.adapot.job.last.successful.rewardcalc_time";
    public static final String YACI_STORE_ADAPOT_JOB_LAST_SUCCESSFUL_STAKE_SNAPSHOT_TIME = "yaci.store.adapot.job.last.successful.stake_snapshot_time";
    public static final String YACI_STORE_ADAPOT_JOB_LAST_SUCCESSFUL_DREP_DISTR_SNAPSHOT_TIME = "yaci.store.adapot.job.last.successful.drep_distr_snapshot_time";
    public static final String YACI_STORE_ADAPOT_JOB_LAST_SUCCESSFUL_UPDATE_REWARD_TIME = "yaci.store.adapot.job.last.successful.update_reward_time";
    public static final String YACI_STORE_ADAPOT_JOB_EPOCH_TOTAL_TIME = "yaci.store.adapot.job.epoch_total_time";
    public static final String YACI_STORE_ADAPOT_JOB_TOTAL_TIME_SUMMARY = "yaci.store.adapot.job.total_time_summary";
    private static final String YACI_STORE_ADAPOT_CURRENT_TREASURY = "yaci.store.adapot.current.treasury";
    private static final String YACI_STORE_ADAPOT_CURRENT_RESERVES = "yaci.store.adapot.current.reserves";
    private static final String YACI_STORE_ADAPOT_JOB_LAST_UNSUCCESSFUL_EPOCH = "yaci.store.adapot.job.last.unsuccessful.epoch";
    private static final String YACI_STORE_ADAPOT_JOB_INPROGRESS_EPOCH = "yaci.store.adapot.job.inprogress.epoch";

    private final AdaPotJobStorage adaPotJobStorage;
    private final AdaPotStorage adaPotStorage;
    private final AdaPotProperties adaPotProperties;
    private final MeterRegistry meterRegistry;
    private AdaPotMetricsFetcher adaPotMetricsFetcher;

    public AdaPotMetricService(AdaPotJobStorage adaPotJobStorage, AdaPotStorage adaPotStorage,
                               MeterRegistry meterRegistry, AdaPotProperties adaPotProperties) {
        this.adaPotJobStorage = adaPotJobStorage;
        this.adaPotStorage = adaPotStorage;
        this.adaPotProperties = adaPotProperties;
        this.meterRegistry = meterRegistry;
        this.adaPotMetricsFetcher = new AdaPotMetricsFetcher();

        if (adaPotProperties.isMetricsEnabled()) {
            registerMetrics();
            adaPotMetricsFetcher.updateAdapotMetrics();
        }
    }

    private void registerMetrics() {

        adaPotMetricsFetcher.initDistributionSummary();

        //Last successful AdaPot job
        Gauge.builder(YACI_STORE_ADAPOT_JOB_LAST_SUCCESSFUL_EPOCH,
                        () -> adaPotMetricsFetcher
                                .getLastSuccessfulAdaPotJob()
                                .map(adaPotJob -> adaPotJob.getEpoch())
                                .orElse(0))
                .description("Most recent successfully completed AdaPot job epoch")
                .register(meterRegistry);

        Gauge.builder(YACI_STORE_ADAPOT_JOB_LAST_SUCCESSFUL_TOTAL_TIME,
                        () -> adaPotMetricsFetcher
                                .getLastSuccessfulAdaPotJob()
                                .map(adaPotJob -> adaPotJob.getTotalTime())
                                .orElse(0L))
                .description("Total time taken for the last successful AdaPot job")
                .register(meterRegistry);

        Gauge.builder(YACI_STORE_ADAPOT_JOB_LAST_SUCCESSFUL_REWARDCALC_TIME,
                        () -> adaPotMetricsFetcher
                                .getLastSuccessfulAdaPotJob()
                                .map(adaPotJob -> adaPotJob.getRewardCalcTime())
                                .orElse(0L))
                .description("Time taken for reward calculation in the last successful AdaPot job")
                .register(meterRegistry);

        Gauge.builder(YACI_STORE_ADAPOT_JOB_LAST_SUCCESSFUL_STAKE_SNAPSHOT_TIME,
                        () -> adaPotMetricsFetcher
                                .getLastSuccessfulAdaPotJob()
                                .map(adaPotJob -> adaPotJob.getStakeSnapshotTime())
                                .orElse(0L))
                .description("Time taken for stake snapshot in the last successful AdaPot job")
                .register(meterRegistry);

        Gauge.builder(YACI_STORE_ADAPOT_JOB_LAST_SUCCESSFUL_DREP_DISTR_SNAPSHOT_TIME,
                        () -> adaPotMetricsFetcher
                                .getLastSuccessfulAdaPotJob()
                                .map(adaPotJob -> adaPotJob.getDrepDistrSnapshotTime())
                                .orElse(0L))
                .description("Time taken for dRep distribution snapshot in the last successful AdaPot job")
                .register(meterRegistry);


        Gauge.builder(YACI_STORE_ADAPOT_JOB_LAST_SUCCESSFUL_UPDATE_REWARD_TIME,
                        () -> adaPotMetricsFetcher
                                .getLastSuccessfulAdaPotJob()
                                .map(adaPotJob -> adaPotJob.getUpdateRewardTime())
                                .orElse(0L))
                .description("Time taken for updating rewards in the last successful AdaPot job")
                .register(meterRegistry);

        Gauge.builder(YACI_STORE_ADAPOT_JOB_LAST_UNSUCCESSFUL_EPOCH,
                        () -> adaPotMetricsFetcher
                                .getLastUnsuccessfulAdaPotJob()
                                .map(AdaPotJob::getEpoch)
                                .orElse(0))
                .description("Most recent unsuccessful AdaPot job epoch")
                .register(meterRegistry);

        Gauge.builder(YACI_STORE_ADAPOT_JOB_INPROGRESS_EPOCH,
                        () -> adaPotMetricsFetcher
                                .getInProgressAdaPotEpoch()
                                .map(AdaPotJob::getEpoch)
                                .orElse(0))
                .description("Current in-progress AdaPot job epoch")
                .register(meterRegistry);


        //Recent treasury and reserves info in the database
        Gauge.builder(YACI_STORE_ADAPOT_CURRENT_TREASURY, () ->
                    adaPotMetricsFetcher.getLastSuccessfulAdaPot()
                            .map(AdaPot::getTreasury)
                            .orElse(BigInteger.ZERO))
                .description("Current treasury amount in the last successful AdaPot")
                .register(meterRegistry);

        Gauge.builder(YACI_STORE_ADAPOT_CURRENT_RESERVES, () ->
                    adaPotMetricsFetcher.getLastSuccessfulAdaPot()
                            .map(AdaPot::getReserves)
                            .orElse(BigInteger.ZERO))
                .description("Current reserves amount in the last successful AdaPot")
                .register(meterRegistry);
    }

    public class AdaPotMetricsFetcher {
        private long lastUpdatedTimeMillis = 0L;
        private Optional<AdaPotJob> lastSuccessfulAdaPotJob;
        private Optional<AdaPotJob> lastUnsuccessfulAdaPotJob = Optional.empty();
        private Optional<AdaPotJob> inProgressAdaPotJob = Optional.empty();
        private Optional<AdaPot> lastSuccessfulAdaPot;

        private List<AdaPotJob> last20SuccessfulJobs = Collections.emptyList();
        private final Map<Integer, Gauge> last20EpochGauges = new HashMap<>();
        private DistributionSummary totalTimeSummary;

        public synchronized void initDistributionSummary() {
            this.totalTimeSummary = DistributionSummary.builder(YACI_STORE_ADAPOT_JOB_TOTAL_TIME_SUMMARY)
                    .description("Summary of AdaPot job total times")
                    .publishPercentiles(0.5, 0.9, 0.99) // p50, p90, p99
                    .register(meterRegistry);
        }

        public Optional<AdaPotJob> getLastSuccessfulAdaPotJob() {
            updateAdapotMetrics();
            return lastSuccessfulAdaPotJob;
        }

        public Optional<AdaPot> getLastSuccessfulAdaPot() {
            updateAdapotMetrics();
            return lastSuccessfulAdaPot;
        }

        public Optional<AdaPotJob> getLastUnsuccessfulAdaPotJob() {
            updateAdapotMetrics();
            return lastUnsuccessfulAdaPotJob;
        }

        public Optional<AdaPotJob> getInProgressAdaPotEpoch() {
            updateAdapotMetrics();
            return inProgressAdaPotJob;
        }

        public synchronized void updateAdapotMetrics() {
            try {
                long now = System.currentTimeMillis();
                if (now - lastUpdatedTimeMillis > adaPotProperties.getMetricsUpdateInterval()) {
                    if (log.isTraceEnabled())
                        log.trace("Updating AdaPot job metrics...");

                    // Fetch latest successful job
                    lastSuccessfulAdaPotJob = adaPotJobStorage.getLatestJobByTypeAndStatus(
                            AdaPotJobType.REWARD_CALC, AdaPotJobStatus.COMPLETED);

                    if (lastSuccessfulAdaPotJob.isPresent()) {
                        lastSuccessfulAdaPot = adaPotStorage.findByEpoch(lastSuccessfulAdaPotJob.get().getEpoch());
                    }

                    var lastStartedAdaPotJob = adaPotJobStorage.getLatestJobByTypeAndStatus(
                                    AdaPotJobType.REWARD_CALC, AdaPotJobStatus.STARTED)
                            .orElse(null);

                    if (lastStartedAdaPotJob != null) {
                        if (lastStartedAdaPotJob.getErrorMessage() == null || lastStartedAdaPotJob.getErrorMessage().isEmpty()) {
                            // If the last started job has no error, it means it's still running
                            lastUnsuccessfulAdaPotJob = Optional.empty();
                            inProgressAdaPotJob = Optional.ofNullable(lastStartedAdaPotJob);
                        } else {
                            // If it has an error, consider it as unsuccessful
                            lastUnsuccessfulAdaPotJob = Optional.ofNullable(lastStartedAdaPotJob);
                            inProgressAdaPotJob = Optional.empty();
                        }
                    }

                    // Fetch last 20 successful jobs
                    List<AdaPotJob> newLast20Jobs = adaPotJobStorage.getRecentCompletedJobs(20);

                    // Remove old gauges that are no longer in new list
                    Set<Integer> newEpochs = newLast20Jobs.stream()
                            .map(AdaPotJob::getEpoch)
                            .collect(Collectors.toSet());

                    Set<Integer> oldEpochs = new HashSet<>(last20EpochGauges.keySet());
                    oldEpochs.removeAll(newEpochs);
                    oldEpochs.forEach(epoch -> {
                        Gauge g = last20EpochGauges.remove(epoch);
                        if (g != null) {
                            if (log.isTraceEnabled())
                                log.trace("Removing old gauge for epoch {}", epoch);
                        }
                    });

                    // Add new gauges and record totalTime into summary
                    newLast20Jobs.forEach(adaPotJob -> {
                        int epoch = adaPotJob.getEpoch();

                        // Register Gauge if not already present
                        if (!last20EpochGauges.containsKey(epoch)) {
                            Gauge gauge = Gauge.builder(YACI_STORE_ADAPOT_JOB_EPOCH_TOTAL_TIME, adaPotJob, job -> job.getTotalTime())
                                    .description("Total time for AdaPot job per epoch")
                                    .tag("epoch", String.valueOf(epoch))
                                    .register(meterRegistry);

                            last20EpochGauges.put(epoch, gauge);
                        }

                        // Also record totalTime into summary (histogram/percentiles)
                        totalTimeSummary.record(adaPotJob.getTotalTime());
                    });

                    // Store latest list
                    last20SuccessfulJobs = newLast20Jobs;
                    lastUpdatedTimeMillis = now;
                }
            } catch (Exception e) {
                log.error("Error updating AdaPot metrics", e);
            }
        }
    }

}

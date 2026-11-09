package com.bloxbean.cardano.yaci.store.adapot.job;

import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJob;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJobExtraInfo;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJobStatus;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJobType;
import com.bloxbean.cardano.yaci.store.adapot.job.storage.AdaPotJobStorage;
import com.bloxbean.cardano.yaci.store.core.annotation.ReadOnly;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

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
    private final AtomicReference<ActiveJob> activeJob = new AtomicReference<>();

    //Package-private so that tests can run without the delays
    long initialDelayMillis = 5000;
    long retryBaseDelayMillis = 10_000;
    long retryMaxDelayMillis = 1_800_000;

    private volatile Thread processorThread;

    private final AdaPotJobStorage adaPotJobStorage;
    private final AdaPotJobProcessor adaPotJobProcessor;

    @Autowired
    public AdaPotJobManager(AdaPotJobStorage adaPotJobStorage, AdaPotJobProcessor adaPotJobProcessor) {
        this(adaPotJobStorage, adaPotJobProcessor, true);
    }

    AdaPotJobManager(AdaPotJobStorage adaPotJobStorage, AdaPotJobProcessor adaPotJobProcessor,
                     boolean startProcessor) {
        this.adaPotJobStorage = adaPotJobStorage;
        this.adaPotJobProcessor = adaPotJobProcessor;

        //TODO -- Add some delay and then start loading jobs to handle rollback during restart of the application
        // Reset jobs that were in 'STARTED' state to 'NOT_STARTED' and load pending jobs
        resetStartedJobs();
        loadPendingJobs();

        if (startProcessor) {
            // Start a virtual thread for job processing
            processorThread = Thread.startVirtualThread(this::processJobs);
        }
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
     * @param slot  slot number
     */
    public void triggerRewardCalcJob(int epoch, long slot, long block) {
        triggerRewardCalcJob(epoch, slot, block, null);
    }

    public void triggerRewardCalcJob(int epoch, long slot, long block, String blockHash) {
        AdaPotJob job = AdaPotJob.builder()
                .epoch(epoch)
                .slot(slot)
                .block(block)
                .blockHash(blockHash)
                .type(AdaPotJobType.REWARD_CALC)
                .status(AdaPotJobStatus.NOT_STARTED)
                .totalTime(0L)
                .rewardCalcTime(0L)
                .updateRewardTime(0L)
                .stakeSnapshotTime(0L)
                .drepDistrSnapshotTime(0L)
                .extraInfo(AdaPotJobExtraInfo.builder()
                        .drepExpiryCalcTime(0L)
                        .govActionStatusCalcTime(0L)
                        .build())
                .build();
        adaPotJobStorage.save(job);
        jobQueue.add(job);
    }

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void handleRollback(RollbackEvent rollbackEvent) {
        long rollbackSlot = rollbackEvent.getRollbackTo().getSlot();

        jobQueue.removeIf(job -> {
            if (job.getSlot() != null && job.getSlot() > rollbackSlot) {
                log.info("Removed queued AdaPot job for epoch {} at slot {} after rollback to slot {}",
                        job.getEpoch(), job.getSlot(), rollbackSlot);
                return true;
            }
            return false;
        });

        ActiveJob runningJob = activeJob.get();
        if (runningJob != null && runningJob.job().getSlot() != null
                && runningJob.job().getSlot() > rollbackSlot) {
            runningJob.cancel();
            log.info("Cancelled running AdaPot job for epoch {} at slot {} after rollback to slot {}",
                    runningJob.job().getEpoch(), runningJob.job().getSlot(), rollbackSlot);
        }
    }

    private void processJobs() {
        while (true) {
            AdaPotJob job;
            try {
                job = jobQueue.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("AdaPot job processor interrupted. Stopping.");
                return;
            }

            log.info("Found reward calc job in queue : {}", job);
            if (!runUntilDone(job))
                return;
        }
    }

    /**
     * Runs a job until it completes, becomes stale or is cancelled by a rollback.
     * <p>
     * A failed job is retried with backoff and never skipped. The next epoch's job starts from the AdaPot
     * this job writes. Moving on after a failure would calculate the next epoch from an incomplete AdaPot,
     * and leaving the loop would stop all later jobs until a restart.
     *
     * @return false only if the thread was interrupted
     */
    boolean runUntilDone(AdaPotJob job) {
        ActiveJob runningJob = activateJob(job);
        try {
            for (int attempt = 1; ; attempt++) {
                if (!sleepUnlessCancelled(initialDelayMillis, runningJob))
                    return false;

                try {
                    if (adaPotJobProcessor.processJob(job, runningJob::isCancelled))
                        return true;
                    log.error("Reward calculation failed for epoch : {} (attempt {}). Retrying in {} s",
                            job.getEpoch(), attempt, retryDelayMillis(attempt) / 1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                } catch (Throwable e) {
                    //Also errors such as StackOverflowError: anything that escapes would end the job thread
                    log.error("Error processing reward calc job for epoch : {} (attempt {}). Retrying in {} s",
                            job.getEpoch(), attempt, retryDelayMillis(attempt) / 1000, e);
                }

                //The job stays active while it waits, so that a rollback can still cancel it
                if (!runningJob.isCancelled() && !sleepUnlessCancelled(retryDelayMillis(attempt), runningJob))
                    return false;

                if (runningJob.isCancelled()) {
                    log.info("AdaPot job for epoch {} at slot {} was cancelled by a rollback. Not retrying",
                            job.getEpoch(), job.getSlot());
                    return true;
                }
            }
        } finally {
            activeJob.compareAndSet(runningJob, null);
        }
    }

    long retryDelayMillis(int attempt) {
        long delay = retryBaseDelayMillis << Math.min(attempt - 1, 20);
        return Math.min(delay, retryMaxDelayMillis);
    }

    /**
     * Sleeps in short slices so that a rollback cancelling the job ends the wait early.
     *
     * @return false if the thread was interrupted
     */
    private boolean sleepUnlessCancelled(long millis, ActiveJob runningJob) {
        long deadline = System.currentTimeMillis() + millis;
        try {
            for (long left = millis; left > 0 && !runningJob.isCancelled(); left = deadline - System.currentTimeMillis()) {
                Thread.sleep(Math.min(left, 1000));
            }
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @PreDestroy
    void stop() {
        Thread thread = processorThread;
        if (thread != null)
            thread.interrupt();
    }

    int queuedJobCount() {
        return jobQueue.size();
    }

    boolean isCancelled(AdaPotJob job) {
        ActiveJob runningJob = activeJob.get();
        return runningJob != null
                && runningJob.key().equals(JobKey.from(job))
                && runningJob.isCancelled();
    }

    private ActiveJob activateJob(AdaPotJob job) {
        ActiveJob runningJob = new ActiveJob(job, JobKey.from(job), new AtomicBoolean(false));
        activeJob.set(runningJob);
        return runningJob;
    }

    private record JobKey(Integer epoch, Long slot, Long block, String blockHash) {
        private static JobKey from(AdaPotJob job) {
            return new JobKey(job.getEpoch(), job.getSlot(), job.getBlock(), job.getBlockHash());
        }
    }

    private record ActiveJob(AdaPotJob job, JobKey key, AtomicBoolean cancelled) {
        private void cancel() {
            cancelled.set(true);
        }

        private boolean isCancelled() {
            return cancelled.get();
        }
    }
}

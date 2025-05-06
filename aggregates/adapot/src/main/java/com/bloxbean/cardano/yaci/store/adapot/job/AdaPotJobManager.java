package com.bloxbean.cardano.yaci.store.adapot.job;

import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJob;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJobExtraInfo;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJobStatus;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJobType;
import com.bloxbean.cardano.yaci.store.adapot.job.storage.AdaPotJobStorage;
import com.bloxbean.cardano.yaci.store.core.annotation.ReadOnly;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

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

    private final AdaPotJobStorage adaPotJobStorage;
    private final AdaPotJobProcessor adaPotJobProcessor;

    public AdaPotJobManager(AdaPotJobStorage adaPotJobStorage, AdaPotJobProcessor adaPotJobProcessor) {
        this.adaPotJobStorage = adaPotJobStorage;
        this.adaPotJobProcessor = adaPotJobProcessor;

        //TODO -- Add some delay and then start loading jobs to handle rollback during restart of the application
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
     * @param slot  slot number
     */
    public void triggerRewardCalcJob(int epoch, long slot, long block) {
        AdaPotJob job = new AdaPotJob(epoch, slot, block, AdaPotJobType.REWARD_CALC, AdaPotJobStatus.NOT_STARTED, 0L, 0L, 0L, 0L, 0L,
                AdaPotJobExtraInfo.builder().drepExpiryCalcTime(0L).govActionStatusCalcTime(0L).build(), null);
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
                    log.error("Error in thread.sleep", e);
                }

                if (job != null) {
                    boolean status = adaPotJobProcessor.processJob(job);
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
}

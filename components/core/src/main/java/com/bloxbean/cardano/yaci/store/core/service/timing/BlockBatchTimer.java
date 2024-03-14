package com.bloxbean.cardano.yaci.store.core.service.timing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Component
@Slf4j
public class BlockBatchTimer {
    private final int NO_OF_BATCHES_TO_SAMPLE = 5;
    private final int MAX_BATCH_SIZE = 500;

    private long batchProcessingTimeThreshold = 3000; //15 second;
    private float changeFactor = 0.3f;

    private StopWatch stopWatch;

    private void start() {
        stopWatch.start("batch-timer");
    }

    public void stop() {
        stopWatch.stop();
    }

    public Pair<Integer, Integer> startBatch(int currentBatchSize, int currentPartitionSize) {
        if (stopWatch == null)
            stopWatch = new StopWatch();

        int taskCount = stopWatch.getTaskCount();
        if (taskCount > NO_OF_BATCHES_TO_SAMPLE) {
            //calculate average time for the last 5 batches
            long totalTimeInMillis = stopWatch.getTotalTimeMillis();
            long averageTime = totalTimeInMillis / NO_OF_BATCHES_TO_SAMPLE;

            int newBatchSize = currentBatchSize;
            int partitionRatio = currentBatchSize / currentPartitionSize;

            if (averageTime > batchProcessingTimeThreshold) {
                //log warning
                log.info("Batch processing time is more than threshold. Average time : " + averageTime);

                int changeFraction = (int) (currentBatchSize * changeFactor);
                newBatchSize = currentBatchSize - changeFraction;

            } else if (averageTime < batchProcessingTimeThreshold / 2) { //less than half of threshold
                int changeFraction = (int) (currentBatchSize * changeFactor);
                newBatchSize = currentBatchSize + changeFraction;
            }

            if (newBatchSize > MAX_BATCH_SIZE) {
                newBatchSize = MAX_BATCH_SIZE;
            }

            log.info("Batch processing time is less than half of threshold. Average time : " + averageTime);
            log.info("Increasing batch size to " + newBatchSize);

            int newPartitionSize = newBatchSize / partitionRatio;

            if (currentBatchSize != newBatchSize) {
                log.info("Batch size changed from {} to {} and partition size from {} to {}", currentBatchSize, newBatchSize, currentPartitionSize, newPartitionSize);
            }

            stopWatch = new StopWatch();
            start();
            return Pair.of(newBatchSize, newPartitionSize);
        } else {
            start();
            return Pair.of(currentBatchSize, currentPartitionSize);
        }
    }


    public void printStats() {
        if (stopWatch != null) {
            log.info(stopWatch.prettyPrint());
        }
    }

}

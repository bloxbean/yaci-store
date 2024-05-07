package com.bloxbean.cardano.yaci.store.account.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;

import java.util.HashMap;
import java.util.Map;

import static com.bloxbean.cardano.yaci.store.account.job.AccountJobConstants.END_PARTITION;
import static com.bloxbean.cardano.yaci.store.account.job.AccountJobConstants.START_PARTITION;

@RequiredArgsConstructor
@Slf4j
public class BalanceHashedBasePartitioner implements Partitioner {

    public Map<String, ExecutionContext> partition(int numThreads) {
        log.info("Partitioning address data into {} partitions", numThreads);

        Map<String, ExecutionContext> partitionMap = new HashMap<>();

        int numPartitions = 100;
        int partitionsPerThread = numPartitions / numThreads;
        int remainingPartitions = numPartitions % numThreads;

        for (int i = 0; i < numThreads; i++) {
            ExecutionContext context = new ExecutionContext();
            int startPartition;
            int endPartition;

            if (i < remainingPartitions) {
                startPartition = i * (partitionsPerThread + 1);
                endPartition = startPartition + partitionsPerThread;
            } else {
                 startPartition = remainingPartitions + i * partitionsPerThread;
                 endPartition = startPartition + partitionsPerThread - 1;
            }

            context.putLong(START_PARTITION, startPartition);
            context.putLong(END_PARTITION, endPartition);
            partitionMap.put("partition" + i, context);
        }

        log.info("Partitions: {}", partitionMap);
        return partitionMap;
    }

}


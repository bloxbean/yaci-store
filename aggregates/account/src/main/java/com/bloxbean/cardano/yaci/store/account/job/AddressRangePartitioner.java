package com.bloxbean.cardano.yaci.store.account.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.Map;

import static com.bloxbean.cardano.yaci.store.account.job.AccountJobConstants.END_OFFSET;
import static com.bloxbean.cardano.yaci.store.account.job.AccountJobConstants.START_OFFSET;

@RequiredArgsConstructor
@Slf4j
public class AddressRangePartitioner implements Partitioner {

    private final JdbcTemplate jdbcTemplate;

    public Map<String, ExecutionContext> partition(int gridSize) {
        log.info("Partitioning address data into {} partitions", gridSize);
        long totalAddresses = jdbcTemplate.queryForObject("SELECT max(id) FROM address", Long.class);
        long partitionSize = totalAddresses / gridSize;

        Map<String, ExecutionContext> result = new HashMap<>();
        for (int i = 0; i < gridSize; i++) {
            ExecutionContext executionContext = new ExecutionContext();
            long startOffset = i * partitionSize;
            // Adjust endOffset to be one less than the startOffset of the next partition
            // For the last partition, it correctly goes to totalAddresses
            long endOffset = (i == gridSize - 1) ? totalAddresses : (startOffset + partitionSize);

            executionContext.putLong(START_OFFSET, startOffset);
            executionContext.putLong(END_OFFSET, endOffset);

            result.put("partition" + i, executionContext);
        }

        log.info("Partitions: " + result);
        return result;
    }

}


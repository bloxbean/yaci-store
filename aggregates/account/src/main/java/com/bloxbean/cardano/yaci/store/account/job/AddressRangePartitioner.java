package com.bloxbean.cardano.yaci.store.account.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
public class AddressRangePartitioner implements Partitioner {

    private final JdbcTemplate jdbcTemplate;

    public Map<String, ExecutionContext> partition(int gridSize) {
        log.info("Partitioning address data into {} partitions", gridSize);
        int totalAddresses = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM address", Integer.class);
        int partitionSize = totalAddresses / gridSize;

        Map<String, ExecutionContext> result = new HashMap<>();
        for (int i = 0; i < gridSize; i++) {
            ExecutionContext executionContext = new ExecutionContext();
            int startOffset = i * partitionSize;
            // Adjust endOffset to be one less than the startOffset of the next partition
            // For the last partition, it correctly goes to totalAddresses
            int endOffset = (i == gridSize - 1) ? totalAddresses : (startOffset + partitionSize) - 1;

            executionContext.putLong("startOffset", startOffset);
            executionContext.putLong("endOffset", endOffset);

            result.put("partition" + i, executionContext);
        }

        log.info("Partitions: " + result);
        return result;
    }

}


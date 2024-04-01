package com.bloxbean.cardano.yaci.store.account.config;

import com.bloxbean.cardano.yaci.store.account.job.AddressAggregationTasklet;
import com.bloxbean.cardano.yaci.store.account.job.AddressRangePartitioner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class AccountBatchConfiguration {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final JdbcTemplate jdbcTemplate;

    @Bean
    public Job accountBalanceJob() {
        return new JobBuilder("addressAggregationJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(accountBalanceMasterStep())
                .build();
    }

    @Bean
    public Step accountBalanceMasterStep() {
        return new StepBuilder("masterStep", jobRepository)
                .partitioner(accountBalanceSlaveStep().getName(), accountBalancePartitioner())
                .partitionHandler(accountBalanceartitionHandler())
                .build();
    }

    @Bean
    public Partitioner accountBalancePartitioner() {
        return new AddressRangePartitioner(jdbcTemplate);
    }

    @Bean
    public TaskExecutorPartitionHandler accountBalanceartitionHandler() {
        TaskExecutorPartitionHandler partitionHandler = new TaskExecutorPartitionHandler();
        partitionHandler.setStep(accountBalanceSlaveStep());
        partitionHandler.setTaskExecutor(accountBalanceTaskExecutor());
        partitionHandler.setGridSize(10); // Adjust based on your needs and system capabilities
        return partitionHandler;
    }

    @Bean
    public Step accountBalanceSlaveStep() {
        return new StepBuilder("slaveStep", jobRepository)
                .tasklet(accountBalanceTasklet(), transactionManager) // Pass the batchSize to the tasklet
                .build();
    }

    @Bean
    public Tasklet accountBalanceTasklet() {
        return new AddressAggregationTasklet(jdbcTemplate);
    }

    @Bean
    public TaskExecutor accountBalanceTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("Batch-");
        executor.initialize();
        return executor;
    }
}

package com.bloxbean.cardano.yaci.store.account.config;

import com.bloxbean.cardano.yaci.store.account.AccountStoreProperties;
import com.bloxbean.cardano.yaci.store.account.job.*;
import com.bloxbean.cardano.yaci.store.account.service.AccountConfigService;
import com.bloxbean.cardano.yaci.store.core.service.StartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
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
    private final AccountConfigService accountConfigService;
    private final StartService startService;
    private final AccountStoreProperties accountStoreProperties;
    private final DSLContext dsl;

    @Bean
    public Job accountBalanceJob() {
        return new JobBuilder("addressAggregationJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(accountBalanceMasterStep())
                .next(stakeAddressBalanceMasterStep())
                .next(accountConfigUpdateStep())
                .build();
    }

    //--- Account Balance Calculation
    @Bean
    public Step accountBalanceMasterStep() {
        return new StepBuilder("accountBalanceCalcStep", jobRepository)
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
        partitionHandler.setTaskExecutor(balanceTaskExecutor());
        partitionHandler.setGridSize(accountStoreProperties.getBalanceCalcJobPartitionSize());
        return partitionHandler;
    }

    @Bean
    public Step accountBalanceSlaveStep() {
        return new StepBuilder("accountBalanceCalculationStep", jobRepository)
                .tasklet(accountBalanceTasklet(), transactionManager) // Pass the batchSize to the tasklet
                .build();
    }

    @Bean
    public Tasklet accountBalanceTasklet() {
        return new AddressAggregationTasklet(accountStoreProperties, dsl);
    }

    //--- Stake Address Balance Calculation
    public Step stakeAddressBalanceMasterStep() {
        return new StepBuilder("stakeAddressBalanceCalcStep", jobRepository)
                .partitioner(stakeAddressBalanceSlaveStep().getName(), stakeAddressBalancePartitioner())
                .partitionHandler(stakeAddressBalancePartitionHandler())
                .build();
    }

    @Bean
    public Partitioner stakeAddressBalancePartitioner() {
        return new StakeAddressRangePartitioner(jdbcTemplate);
    }

    @Bean
    public TaskExecutorPartitionHandler stakeAddressBalancePartitionHandler() {
        TaskExecutorPartitionHandler partitionHandler = new TaskExecutorPartitionHandler();
        partitionHandler.setStep(stakeAddressBalanceSlaveStep());
        partitionHandler.setTaskExecutor(balanceTaskExecutor());
        partitionHandler.setGridSize(accountStoreProperties.getBalanceCalcJobPartitionSize());
        return partitionHandler;
    }

    @Bean
    public Step stakeAddressBalanceSlaveStep() {
        return new StepBuilder("stakeAddressBalanceCalculationStep", jobRepository)
                .tasklet(stakeAddressBalanceTasklet(), transactionManager) // Pass the batchSize to the tasklet
                .build();
    }

    @Bean
    public Tasklet stakeAddressBalanceTasklet() {
        return new StakeAddressAggregationTasklet(accountStoreProperties, dsl);
    }

    @Bean
    public Step accountConfigUpdateStep() {
        return new StepBuilder("accountConfigUpdateStep", jobRepository)
                .tasklet(accountConfigUpdateTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet accountConfigUpdateTasklet() {
        return new AccountConfigUpdateTasklet(accountConfigService, startService);
    }

    @Bean
    public TaskExecutor balanceTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("Batch-");
        executor.initialize();
        return executor;
    }
}

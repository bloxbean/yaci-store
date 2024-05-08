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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@ConditionalOnProperty(name = "store.account.balance-calc-batch-mode", havingValue = "tx-amount", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class HashedBaseBatchConfiguration {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final JdbcTemplate jdbcTemplate;
    private final AccountConfigService accountConfigService;
    private final StartService startService;
    private final AccountStoreProperties accountStoreProperties;
    private final DSLContext dsl;
    private final PlatformTransactionManager platformTransactionManager;

    @Bean
    @Primary
    public Job balanceByHashedBaseJob() {
        return new JobBuilder("balanceByHashedBaseJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(balanceByHashedBaseMasterStep())
                .next(accountConfigUpdateForBalanceHashedBaseStep())
                .build();
    }


    //--- Account Balance Calculation
    @Bean
    public Step balanceByHashedBaseMasterStep() {
        return new StepBuilder("balanceByHashedBaseCalcStep", jobRepository)
                .partitioner(balanceByHashedBaseSlaveStep().getName(), takeBalanceByHashedBasePartitioner())
                .partitionHandler(takeBalanceHashedBasePartitionHandler())
                .build();
    }

    @Bean
    public Partitioner takeBalanceByHashedBasePartitioner() {
        return new BalanceHashedBasePartitioner(accountStoreProperties);
    }

    @Bean
    public TaskExecutorPartitionHandler takeBalanceHashedBasePartitionHandler() {
        TaskExecutorPartitionHandler partitionHandler = new TaskExecutorPartitionHandler();
        partitionHandler.setStep(balanceByHashedBaseSlaveStep());
        partitionHandler.setTaskExecutor(balanceHashedBaseTaskExecutor());
        partitionHandler.setGridSize(accountStoreProperties.getBalanceCalcJobPartitionSize());
        return partitionHandler;
    }

    @Bean
    public Step balanceByHashedBaseSlaveStep() {
        return new StepBuilder("balanceByHashedBaseCalculationStep", jobRepository)
                .tasklet(balanceHashedBaseTasklet(), transactionManager) // Pass the batchSize to the tasklet
                .build();
    }

    @Bean
    public Tasklet balanceHashedBaseTasklet() {
        return new BalanceByHashedBaseAggregationTasklet(accountStoreProperties, dsl, platformTransactionManager);
    }

    @Bean
    public Step accountConfigUpdateForBalanceHashedBaseStep() {
        return new StepBuilder("accountConfigUpdateForBalanceHashedBaseStep", jobRepository)
                .tasklet(balanceHasedBaseConfigUpdateTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet balanceHasedBaseConfigUpdateTasklet() {
        return new BalanceHashedBaseConfigUpdateTasklet(accountConfigService, startService, platformTransactionManager);
    }

    @Bean
    public TaskExecutor balanceHashedBaseTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("Balance-HashedBase-Batch-");
        executor.initialize();
        return executor;
    }
}

package com.bloxbean.cardano.yaci.store.adapot.service;

import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import static com.bloxbean.cardano.yaci.store.adapot.jooq.Tables.EPOCH_STAKE;

@Service
public class EpochStakeService {
    private final DSLContext dsl;
    private final PlatformTransactionManager transactionManager;
    private TransactionTemplate transactionTemplate;

    @Value("${store.adapot.epoch-stake-pruning-batch-size:3000}")
    private int pruningBatchSize = 3000;

    public EpochStakeService(DSLContext dsl, PlatformTransactionManager transactionManager) {
        this.dsl = dsl;
        this.transactionManager = transactionManager;

        init();
    }

    void init() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public int deleteByEpochLessThan(int epoch) {
        int count;
        int totalCount = 0;

        int limit = pruningBatchSize;

        do {
            count = transactionTemplate.execute(status ->
                    dsl.deleteFrom(EPOCH_STAKE)
                            .where(EPOCH_STAKE.EPOCH.lessThan(epoch))
                            .limit(limit)
                            .execute()
            );

            totalCount += count;
        } while (count > 0);

        return totalCount;
    }
}

package com.bloxbean.cardano.yaci.store.adapot.service;


import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import static com.bloxbean.cardano.yaci.store.adapot.jooq.Tables.REWARD;
import static com.bloxbean.cardano.yaci.store.transaction.jooq.Tables.WITHDRAWAL;

@Service
public class RewardService {
    private final DSLContext dsl;
    private final PlatformTransactionManager transactionManager;
    private TransactionTemplate transactionTemplate;

    @Value("${store.adapot.reward-pruning-batch-size:3000}")
    private int pruningBatchSize = 3000;

    public RewardService(DSLContext dsl, PlatformTransactionManager transactionManager) {
        this.dsl = dsl;
        this.transactionManager = transactionManager;

        init();
    }

    void init() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public int deleteWithdrawnRewards(long slot) {
        int count;
        int totalCount = 0;

        int limit = pruningBatchSize;

        do {
            count = transactionTemplate.execute(status ->
                    dsl.deleteFrom(REWARD)
                            .where(REWARD.SLOT.lt(slot)
                                    .and(REWARD.ADDRESS.in(
                                            dsl.select(WITHDRAWAL.ADDRESS)
                                                    .from(WITHDRAWAL)
                                                    .where(WITHDRAWAL.ADDRESS.eq(REWARD.ADDRESS))
                                                    .and(WITHDRAWAL.SLOT.gt(REWARD.SLOT))
                                                    .and(WITHDRAWAL.SLOT.lt(slot))
                                    ))
                            )
                            .limit(limit)
                            .execute()
            );

            totalCount += count;
        } while (count > 0);

        return totalCount;
    }
}

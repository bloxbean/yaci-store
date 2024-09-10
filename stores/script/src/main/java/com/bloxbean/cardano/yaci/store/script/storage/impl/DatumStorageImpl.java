package com.bloxbean.cardano.yaci.store.script.storage.impl;

import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.executor.ParallelExecutor;
import com.bloxbean.cardano.yaci.store.common.util.StringUtil;
import com.bloxbean.cardano.yaci.store.events.internal.CommitEvent;
import com.bloxbean.cardano.yaci.store.script.domain.Datum;
import com.bloxbean.cardano.yaci.store.script.storage.DatumStorage;
import com.bloxbean.cardano.yaci.store.script.storage.impl.repository.DatumRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.yaci.store.common.util.ListUtil.partition;
import static com.bloxbean.cardano.yaci.store.script.jooq.Tables.DATUM;

@Slf4j
public class DatumStorageImpl implements DatumStorage {
    private final DatumRepository datumRepository;
    private final DSLContext dsl;
    private final ParallelExecutor executorHelper;
    private final StoreProperties storeProperties;

    private final PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    private Set<Datum> datumCache = Collections.synchronizedSet(new HashSet<>());

    public DatumStorageImpl(DatumRepository datumRepository, DSLContext dsl, ParallelExecutor executorHelper, StoreProperties storeProperties,
                            PlatformTransactionManager transactionManager) {
        this.datumRepository = datumRepository;
        this.dsl = dsl;
        this.executorHelper = executorHelper;
        this.storeProperties = storeProperties;
        this.transactionManager = transactionManager;

        init();
    }

    public void init() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    @Transactional
    public void saveAll(Collection<Datum> datumList) {
        if (datumList == null || datumList.isEmpty()) return;

        datumList = datumList.stream()
                        .filter(datum -> !StringUtil.isEmpty(datum.getDatum()))
                                .collect(Collectors.toSet());

        datumCache.addAll(datumList);
    }

    @Transactional
    public void handleCommit(CommitEvent commitEvent) {
        try {
            if (datumCache.size() == 0)
                return;

            LocalDateTime localDateTime = LocalDateTime.now();

            if (datumCache.size() <= storeProperties.getDbBatchSize() || !storeProperties.isDbParallelInsert()) {
                saveDatumsInPartition(datumCache.stream().toList(), localDateTime);
            } else {
               List<List<Datum>> partitions = partition(datumCache.stream().toList(), storeProperties.getDbBatchSize());
               List<Future> futures = new ArrayList<>();
                for (var partition: partitions) {
                    var future = CompletableFuture.supplyAsync(() -> {
                        saveDatumsInPartition(partition, localDateTime);
                        return true;
                    }, executorHelper.getVirtualThreadExecutor());

                    futures.add(future);
                }

               var allCf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
               allCf.join();
            }

            if (datumCache.size() > 1000)
                log.info("Datum Size: " + datumCache.size());

        } finally {
            datumCache.clear();
        }
    }

    private void saveDatumsInPartition(List<Datum> partition, LocalDateTime localDateTime) {
        transactionTemplate.execute(status -> {
            dsl.batched(c -> {
                for (var datum : partition) {
                    c.dsl().insertInto(DATUM)
                            .set(DATUM.HASH, datum.getHash())
                            .set(DATUM.DATUM_, datum.getDatum())
                            .set(DATUM.CREATED_AT_TX, datum.getCreatedAtTx())
                            .set(DATUM.CREATE_DATETIME, localDateTime)
                            .set(DATUM.UPDATE_DATETIME, localDateTime)
                            .onDuplicateKeyIgnore()
                            .execute();
                }
            });

            return null;
        });
    }
}

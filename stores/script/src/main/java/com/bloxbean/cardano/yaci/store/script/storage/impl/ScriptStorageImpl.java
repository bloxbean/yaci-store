package com.bloxbean.cardano.yaci.store.script.storage.impl;

import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.executor.ParallelExecutor;
import com.bloxbean.cardano.yaci.store.events.internal.CommitEvent;
import com.bloxbean.cardano.yaci.store.plugin.aspect.Plugin;
import com.bloxbean.cardano.yaci.store.script.domain.Script;
import com.bloxbean.cardano.yaci.store.script.storage.ScriptStorage;
import com.bloxbean.cardano.yaci.store.script.storage.impl.mapper.ScriptMapper;
import com.bloxbean.cardano.yaci.store.script.storage.impl.repository.ScriptRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.JSON;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static com.bloxbean.cardano.yaci.store.common.util.ListUtil.partition;
import static com.bloxbean.cardano.yaci.store.script.jooq.Tables.SCRIPT;

@Slf4j
public class ScriptStorageImpl implements ScriptStorage {
    private final static String PLUGIN_SCRIPT_SAVE = "script.save";

    private final ScriptRepository scriptRepository;
    private final ScriptMapper scriptMapper;
    private final DSLContext dsl;
    private final ParallelExecutor executorHelper;
    private final StoreProperties storeProperties;

    private final PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    private Set<Script> scriptCache = Collections.synchronizedSet(new HashSet<>());

    public ScriptStorageImpl(ScriptRepository scriptRepository, ScriptMapper scriptMapper, DSLContext dsl, ParallelExecutor executorHelper,
                             StoreProperties storeProperties, PlatformTransactionManager transactionManager) {
        this.scriptRepository = scriptRepository;
        this.scriptMapper = scriptMapper;
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
    @Plugin(key = PLUGIN_SCRIPT_SAVE)
    public List<Script> saveScripts(List<Script> scripts) {
        if (scripts != null && !scripts.isEmpty())
            scriptCache.addAll(scripts);

        return scripts;
    }

    @Transactional
    public void handleCommit(CommitEvent commitEvent) {
        try {
            if (scriptCache.size() == 0)
                return;

            LocalDateTime localDateTime = LocalDateTime.now();

            if (scriptCache.size() <= storeProperties.getDbBatchSize() || !storeProperties.isDbParallelInsert()) {
                saveScriptsInPartition(scriptCache.stream().toList(), localDateTime);
            } else {
                List<List<Script>> partitions = partition(scriptCache.stream().toList(), storeProperties.getDbBatchSize());
                List<Future> futures = new ArrayList<>();
                for (var partition : partitions) {
                    var future = CompletableFuture.supplyAsync(() -> {
                        saveScriptsInPartition(partition, localDateTime);
                        return true;
                    }, executorHelper.getVirtualThreadExecutor());

                    futures.add(future);
                }

                var allCf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
                allCf.join();
            }

            if (scriptCache.size() > 1000)
                log.info("Script Size: " + scriptCache.size());
        } finally {
            scriptCache.clear();
        }
    }

    private void saveScriptsInPartition(List<Script> partition, LocalDateTime localDateTime) {
        transactionTemplate.execute(status -> {
            dsl.batched(c -> {
                for (var script : partition) {
                    c.dsl().insertInto(SCRIPT)
                            .set(SCRIPT.SCRIPT_HASH, script.getScriptHash())
                            .set(SCRIPT.SCRIPT_TYPE, script.getScriptType().toString())
                            .set(SCRIPT.CONTENT, JSON.valueOf(script.getContent()))
                            .set(SCRIPT.CREATE_DATETIME, localDateTime)
                            .set(SCRIPT.UPDATE_DATETIME, localDateTime)
                            .onDuplicateKeyIgnore()
                            .execute();
                }
            });

            return null;
        });
    }
}

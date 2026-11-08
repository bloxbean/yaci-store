package com.bloxbean.cardano.yaci.store.core.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionExecution;
import org.springframework.transaction.TransactionExecutionListener;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fences the commit of every transaction begun by a thread of a sync session (#1129).
 * <p>
 * The session checks in the publishers run before the commit. A new session can start right after the last check,
 * while the old thread is still committing. So, right before the commit, the session is checked once more under the
 * read lock of {@link SyncSessionGuard}: a stale transaction is rolled back, and a current one keeps the session from
 * changing until its commit is done. Only the commit is inside the lock, not the listener code, so a listener that
 * hangs does not hold up a restart; when it resumes, its transaction is rolled back here.
 * <p>
 * Spring Boot registers this listener with the transaction manager. It applies to transactions begun on a thread
 * marked with a session: the chain-sync and range-fetch callbacks, and the publishers' worker threads
 * ({@link SyncSessionGuard#propagating}). Threads a store starts on its own are not marked.
 */
@Component
@Slf4j
public class SyncSessionTransactionFence implements TransactionExecutionListener {
    private final SyncSessionGuard sessionGuard;
    private final Map<TransactionExecution, Long> sessions = new ConcurrentHashMap<>();
    private final Map<TransactionExecution, Runnable> releases = new ConcurrentHashMap<>();

    public SyncSessionTransactionFence(SyncSessionGuard sessionGuard) {
        this.sessionGuard = sessionGuard;
    }

    @Override
    public void afterBegin(TransactionExecution transaction, Throwable beginFailure) {
        Long session = sessionGuard.callerSession();
        if (beginFailure == null && session != null)
            sessions.put(transaction, session);
    }

    @Override
    public void beforeCommit(TransactionExecution transaction) {
        Long session = sessions.remove(transaction);
        if (session == null)
            return;
        //Throws if the session is stale, and the transaction is rolled back
        releases.put(transaction, sessionGuard.enterCommit(session));
        //Release right after the commit, before the after-commit listeners run
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public int getOrder() {
                    return Ordered.HIGHEST_PRECEDENCE;
                }

                @Override
                public void afterCommit() {
                    release(transaction);
                }

                @Override
                public void afterCompletion(int status) {
                    release(transaction);
                }
            });
        }
    }

    @Override
    public void afterCommit(TransactionExecution transaction, Throwable commitFailure) {
        release(transaction);
    }

    @Override
    public void afterRollback(TransactionExecution transaction, Throwable rollbackFailure) {
        sessions.remove(transaction);
        release(transaction);
    }

    private void release(TransactionExecution transaction) {
        Runnable release = releases.remove(transaction);
        if (release != null)
            release.run();
    }
}

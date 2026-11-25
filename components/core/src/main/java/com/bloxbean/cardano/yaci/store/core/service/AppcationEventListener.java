package com.bloxbean.cardano.yaci.store.core.service;

import com.bloxbean.cardano.yaci.store.common.cache.MVStoreFactory;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.service.CursorService;
import com.bloxbean.cardano.yaci.store.core.annotation.ReadOnly;
import com.bloxbean.cardano.yaci.store.events.internal.RequiredSyncRestartEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.sql.SQLRecoverableException;
import java.sql.SQLTransientException;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        value="store.sync-auto-start",
        havingValue = "true",
        matchIfMissing = true
)
@ReadOnly(false)
@Slf4j
public class AppcationEventListener {
    private final StartService startService;
    private final CursorService cursorService;
    private final StoreProperties storeProperties;
    private final BlockFetchService blockFetchService;
    private final ApplicationEventPublisher publisher;

    @EventListener
    public void initialize(ApplicationReadyEvent applicationReadyEvent) {
        init();
        try {
            startService.start();
        } catch (RuntimeException e) {
            // An exception here makes Spring Boot close the application. When auto-restart is on and the failure
            // can pass on its own (the node does not answer yet, the database or the network is down), treat it
            // like a failed restart instead: leave the sync in the error state and let the restart processor try
            // again. Other failures (for example, a configuration error) still stop the application.
            if (!storeProperties.isAutoRestartEnabled() || !isTransient(e))
                throw e;

            log.error("Sync could not be started. Auto-restart will try again.", e);
            blockFetchService.markRestartFailed();
            publisher.publishEvent(RequiredSyncRestartEvent.builder()
                    .reason("StartFailed")
                    .errorCode("START_FAILED")
                    .timestamp(System.currentTimeMillis())
                    .source("AppcationEventListener")
                    .details(String.valueOf(e.getMessage()))
                    .build());
        }
    }

    private void init() {

        if (storeProperties.isMvstoreEnabled()) {
            //check current cursor
            var cursor = cursorService.getCursor();

            //Initialize MVStore
            File mvStoreFolder = new File(storeProperties.getMvstorePath());
            if (!mvStoreFolder.exists()) {
                mvStoreFolder.mkdirs();
            }

            File dbFile = new File(mvStoreFolder, "yaci_store.mv.db");
            if (cursor.isEmpty() && dbFile.exists()) {
                dbFile.delete(); //delete existing db file
            }

            MVStoreFactory.getInstance().init(dbFile.getAbsolutePath());
        }
    }

    /**
     * Failures that can pass on their own: the node does not answer, or the database or the network cannot be
     * reached. Not SQL or schema errors, which a retry cannot fix.
     */
    static boolean isTransient(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (t instanceof NodeNotAnsweringException
                    || t instanceof TransientDataAccessException
                    || t instanceof RecoverableDataAccessException
                    || t instanceof DataAccessResourceFailureException
                    || t instanceof CannotCreateTransactionException
                    || t instanceof SQLTransientException
                    || t instanceof SQLRecoverableException
                    || t instanceof SocketException
                    || t instanceof SocketTimeoutException
                    || t instanceof UnknownHostException)
                return true;
            // SQL state class 08: connection exception
            if (t instanceof SQLException sqlException && sqlException.getSQLState() != null
                    && sqlException.getSQLState().startsWith("08"))
                return true;
        }
        return false;
    }
}

package com.bloxbean.cardano.yaci.store.core.service;

import com.bloxbean.cardano.yaci.store.common.cache.MVStoreFactory;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.service.CursorService;
import com.bloxbean.cardano.yaci.store.core.annotation.ReadOnly;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.File;

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

    @EventListener
    public void initialize(ApplicationReadyEvent applicationReadyEvent) {
        init();
        startService.start();
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
}

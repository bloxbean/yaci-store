package com.bloxbean.cardano.yaci.store.core.service.local;

import com.bloxbean.cardano.yaci.helper.LocalClientProvider;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Optional;

/**
 * This class manages the LocalClientProvider instances. It creates new instances if pool is disabled. If pool is enabled,
 * it borrows the LocalClientProvider from the pool and returns it back to the pool after use.
 * <p>
 * The LocalClientProvider is used to query local ledger state, monitor mempool transactions or submit transaction using Node-to-client.
 * </p>
 * <p>
 * To enable pool, set store.cardano.n2c-pool-enabled=true in application.properties
 * </p>
 */
@Slf4j
public class LocalClientProviderManager {
    private final GenericObjectPool<LocalClientProvider> localClientProviderPool;

    private String n2cSocket;
    private String n2cHost;
    private int n2cPort;
    private long protocolMagic;

    private boolean isNodeSocketFileEnabled;
    private boolean isNodeHostEnabled;

    private boolean isPoolEnabled;

    public LocalClientProviderManager(@Qualifier("localClientProviderPool") @Nullable GenericObjectPool<LocalClientProvider> localClientProviderPool,
                                      StoreProperties storeProperties) {
        this.localClientProviderPool = localClientProviderPool;

        this.n2cSocket = storeProperties.getN2cNodeSocketPath();
        this.n2cHost = storeProperties.getN2cHost();
        this.n2cPort = storeProperties.getN2cPort();
        this.protocolMagic = storeProperties.getProtocolMagic();

        this.isPoolEnabled = storeProperties.isN2cPoolEnabled();

        if (n2cSocket != null && !n2cSocket.isEmpty()) {
            isNodeSocketFileEnabled = true;
        } else if (n2cHost != null && !n2cHost.isEmpty()) {
            isNodeHostEnabled = true;
        }

        log.info("LocalClientProviders initialized >>>");
        if (localClientProviderPool != null)
            log.info("LocalClientProvider pool enabled");
        else
            log.info("LocalClientProvider pool disabled");
    }

    /**
     * Get LocalClientProvider instance. If pool is enabled, it borrows the LocalClientProvider from the pool.
     * If pool is disabled, it creates a new LocalClientProvider instance.
     * @return LocalClientProvider
     */
    public Optional<LocalClientProvider> getLocalClientProvider() {
        if (isPoolEnabled) {
            try {
                var provider = localClientProviderPool.borrowObject();
                if (log.isDebugEnabled())
                    log.debug("Borrowed LocalClientProvider from pool");
                return Optional.of(provider);
            } catch (Exception e) {
                throw new IllegalStateException("Error getting LocalClientProvider from pool", e);
            }
        } else {
            if (log.isDebugEnabled())
                log.debug("Creating new LocalClientProvider");
            return getNewLocalClientProvider();
        }
    }

    /**
     * Creates a new LocalClientProvider instance
     * @return LocalClientProvider
     */
    public Optional<LocalClientProvider> getNewLocalClientProvider() {
        LocalClientProvider localClientProvider = null;
        if (isNodeSocketFileEnabled) {
            localClientProvider = new LocalClientProvider(n2cSocket, protocolMagic);
        } else if (isNodeHostEnabled) {
            localClientProvider = new LocalClientProvider(n2cHost, n2cPort, protocolMagic);
        } else {
            log.error("LocalClientProvider not initialized. Please check the configuration");
        }

        localClientProvider.suppressConnectionInfoLog(true);
        localClientProvider.start();

        return Optional.ofNullable(localClientProvider);
    }

    /**
     * Returns the LocalClientProvider to the pool if pool is enabled. If pool is disabled, it shuts down the LocalClientProvider.
     * @param localClientProvider
     */
    public void close(LocalClientProvider localClientProvider) {
        if (isPoolEnabled) {
            if (log.isDebugEnabled())
                log.debug("Returning LocalClientProvider to pool");
            try {
                localClientProviderPool.returnObject(localClientProvider);
            } catch (Exception e) {
                log.error("Error returning LocalClientProvider to pool", e);
            }
        } else {
            if(localClientProvider != null) {
                localClientProvider.shutdown();
            }
        }

    }
}

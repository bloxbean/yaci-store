package com.bloxbean.cardano.yaci.store.core.service.local;

import com.bloxbean.cardano.yaci.core.protocol.localstate.queries.BlockHeightQuery;
import com.bloxbean.cardano.yaci.helper.LocalClientProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import java.time.Duration;

/**
 * Factory class to create LocalClientProvider object and manage it in a pool
 */
@Slf4j
public class LocalClientProviderPoolObjectFactory extends BasePooledObjectFactory<LocalClientProvider> {

    private String nodeSocketPath;
    private String host;
    private int port;
    private long protocolMagic;

    private boolean isNodeSocketPathEnable;
    private boolean isNodeHostPortEnable;

    public LocalClientProviderPoolObjectFactory(String host, int port, long protocolMagic) {
        this.host = host;
        this.port = port;
        this.protocolMagic = protocolMagic;
        this.isNodeHostPortEnable = true;
    }

    public LocalClientProviderPoolObjectFactory(String nodeSocketPath, long protocolMagic) {
        this.nodeSocketPath = nodeSocketPath;
        this.protocolMagic = protocolMagic;
        this.isNodeSocketPathEnable = true;
    }

    @Override
    public void destroyObject(PooledObject<LocalClientProvider> p) {
        p.getObject().shutdown();
    }

    @Override
    public boolean validateObject(PooledObject<LocalClientProvider> p) {
        try {
            var blockHeight = p.getObject().getLocalStateQueryClient().executeQuery(new BlockHeightQuery()).block(Duration.ofSeconds(3));
            if (log.isDebugEnabled())
                log.debug("Validating LocalClientProvider. BlockHeight: {}", blockHeight);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public PooledObject<LocalClientProvider> wrap(LocalClientProvider obj) {
        return new DefaultPooledObject<>(obj);
    }

    @Override
    public void activateObject(PooledObject<LocalClientProvider> p) {
        if (log.isDebugEnabled())
            log.debug("Activating LocalClientProvider");
    }

    @Override
    public LocalClientProvider create() throws Exception {
        LocalClientProvider localClientProvider = null;
        if(isNodeSocketPathEnable) {
            localClientProvider = new LocalClientProvider(nodeSocketPath, protocolMagic);
        } else if (isNodeHostPortEnable) {
            localClientProvider = new LocalClientProvider(host, port, protocolMagic);
        } else {
            throw new IllegalArgumentException("Either nodeSocketPath or nodeHost/nodePort should be enabled");
        }

        if (localClientProvider != null) {
            localClientProvider.suppressConnectionInfoLog(true);
            localClientProvider.start();
        }

        return localClientProvider;

    }

}


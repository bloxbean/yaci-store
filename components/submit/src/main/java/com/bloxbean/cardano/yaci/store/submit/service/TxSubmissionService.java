package com.bloxbean.cardano.yaci.store.submit.service;

import com.bloxbean.cardano.yaci.core.common.TxBodyType;
import com.bloxbean.cardano.yaci.core.protocol.localtx.model.TxSubmissionRequest;
import com.bloxbean.cardano.yaci.helper.LocalTxSubmissionClient;
import com.bloxbean.cardano.yaci.helper.model.TxResult;
import com.bloxbean.cardano.yaci.store.core.service.local.LocalClientProviderManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Transaction submission service using local node connection (N2C protocol).
 * This is the preferred submission method when a local node is available.
 * Implements TxSubmitter interface for lifecycle tracking integration.
 */
@Service
@ConditionalOnExpression("'${store.cardano.n2c-node-socket-path:}' != '' || '${store.cardano.n2c-host:}' != ''")
@ConditionalOnBean(LocalClientProviderManager.class)
@ConditionalOnMissingBean(OgmiosService.class)
@Slf4j
public class TxSubmissionService implements TxSubmitter {
    private final LocalClientProviderManager localClientProviderManager;

    public TxSubmissionService(LocalClientProviderManager localClientProviderManager) {
        this.localClientProviderManager = localClientProviderManager;
    }

    /**
     * Submit transaction via local node (original method for backward compatibility).
     */
    public TxResult submitTx(TxBodyType txBodyType, byte[] txBytes) {
        var localClientProvider = localClientProviderManager.getLocalClientProvider()
                .orElseThrow(() -> new RuntimeException("LocalClientProvider not available. Check n2c configuration."));

        try {
            LocalTxSubmissionClient txSubmissionClient = localClientProvider.getTxSubmissionClient();
            TxSubmissionRequest txSubmissionRequest = new TxSubmissionRequest(txBodyType, txBytes);
            Mono<TxResult> txResultMono = txSubmissionClient.submitTx(txSubmissionRequest);
            return txResultMono.block(Duration.ofSeconds(10));
        } finally {
            localClientProviderManager.close(localClientProvider);
        }
    }

    /**
     * TxSubmitter interface implementation.
     * Submits transaction and returns transaction hash.
     */
    @Override
    public String submitTx(byte[] cborTx) throws Exception {
        TxResult txResult = submitTx(TxBodyType.CONWAY, cborTx);
        
        if (txResult.isAccepted()) {
            return txResult.getTxHash();
        } else {
            throw new RuntimeException(txResult.getErrorCbor());
        }
    }

}

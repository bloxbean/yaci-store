package com.bloxbean.cardano.yaci.store.submit.service;

import com.bloxbean.cardano.yaci.core.common.TxBodyType;
import com.bloxbean.cardano.yaci.core.protocol.localtx.model.TxSubmissionRequest;
import com.bloxbean.cardano.yaci.helper.LocalTxSubmissionClient;
import com.bloxbean.cardano.yaci.helper.model.TxResult;
import com.bloxbean.cardano.yaci.store.core.service.local.LocalClientProviderManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@ConditionalOnExpression("'${store.cardano.n2c-node-socket-path:}' != '' || '${store.cardano.n2c-host:}' != ''")
@ConditionalOnBean(LocalClientProviderManager.class)
@Slf4j
public class TxSubmissionService {
    private final LocalClientProviderManager localClientProviderManager;

    public TxSubmissionService(LocalClientProviderManager localClientProviderManager) {
        this.localClientProviderManager = localClientProviderManager;
    }

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

}

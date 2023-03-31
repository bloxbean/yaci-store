package com.bloxbean.cardano.yaci.store.submit.service;

import com.bloxbean.cardano.yaci.core.common.TxBodyType;
import com.bloxbean.cardano.yaci.core.protocol.localtx.model.TxSubmissionRequest;
import com.bloxbean.cardano.yaci.helper.LocalClientProvider;
import com.bloxbean.cardano.yaci.helper.LocalTxSubmissionClient;
import com.bloxbean.cardano.yaci.helper.model.TxResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@ConditionalOnExpression("'${store.cardano.n2c-node-socket-path:}' != '' || '${store.cardano.n2c-host:}' != ''")
@Slf4j
public class TxSubmissionService {
    private final LocalClientProvider localClientProvider;
    private final LocalTxSubmissionClient txSubmissionClient;

    private TxSubmissionService(LocalClientProvider localClientProvider) {
        this.localClientProvider = localClientProvider;
        this.txSubmissionClient = localClientProvider.getTxSubmissionClient();
    }

    public TxResult submitTx(TxBodyType txBodyType, byte[] txBytes) {
        TxSubmissionRequest txSubmissionRequest = new TxSubmissionRequest(txBodyType, txBytes);
        Mono<TxResult> txResultMono = txSubmissionClient.submitTx(txSubmissionRequest);
        return txResultMono.block(Duration.ofSeconds(10));
    }

}

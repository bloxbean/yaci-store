package com.bloxbean.cardano.yaci.store.submit.service;

import com.bloxbean.cardano.client.api.exception.ApiException;
import com.bloxbean.cardano.client.api.model.EvaluationResult;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.backend.ogmios.http.OgmiosBackendService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnProperty(name = "store.cardano.ogmios-url")
@Slf4j
public class OgmiosService {
    private String ogmiosUrl;
    private OgmiosBackendService ogmiosBackendService;

    public OgmiosService(Environment env) {
        this.ogmiosUrl = env.getProperty("store.cardano.ogmios-url");
        this.ogmiosBackendService = new OgmiosBackendService(ogmiosUrl);
        log.info("<< Ogmios Service initialized >> " + ogmiosUrl);
    }

    public Result<String> submitTx(byte[] cborTx) throws ApiException {
        Result<String> result = ogmiosBackendService.getTransactionService().submitTransaction(cborTx);
        return result;
    }

    public Result<List<EvaluationResult>> evaluateTx(byte[] cborTx) throws ApiException {
        if (log.isDebugEnabled())
            log.debug("Evaluating tx ..." + ogmiosUrl);

        try {
            Result<List<EvaluationResult>> result = ogmiosBackendService.getTransactionService().evaluateTx(cborTx);

            if (log.isDebugEnabled())
                log.debug("Evaluation Result: " + result);
            return result;
        } catch (Exception e) {
            log.error("Error evaluating tx: ", e);
            throw new ApiException("Error evaluating tx: " + e.getMessage());
        }
    }

    public String getOgmiosUrl() {
        return ogmiosUrl;
    }
}

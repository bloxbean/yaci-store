package com.bloxbean.cardano.yaci.store.submit.service;

import com.bloxbean.cardano.client.api.exception.ApiException;
import com.bloxbean.cardano.client.api.model.EvaluationResult;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.backend.ogmios.OgmiosBackendService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnProperty(name = "store.cardano.ogmios-url")
public class OgmiosService {
    private String ogmiosUrl;
    private OgmiosBackendService ogmiosBackendService;

    public OgmiosService(@Value("${store.cardano.ogmios-url:#{null}}") String ogmiosUrl) {
        this.ogmiosUrl = ogmiosUrl;
        ogmiosBackendService = new OgmiosBackendService(ogmiosUrl);
    }

    public Result<String> submitTx(byte[] cborTx) throws ApiException {
        Result<String> result = ogmiosBackendService.getTransactionService().submitTransaction(cborTx);
        return result;
    }

    public Result<List<EvaluationResult>> evaluateTx(byte[] cborTx) throws ApiException {
        Result<List<EvaluationResult>> result = ogmiosBackendService.getTransactionService().evaluateTx(cborTx);
        return result;
    }

    public String getOgmiosUrl() {
        return ogmiosUrl;
    }
}

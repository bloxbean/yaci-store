package com.bloxbean.cardano.yaci.store.blockfrost.transaction.service;

import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.yaci.core.common.TxBodyType;
import com.bloxbean.cardano.yaci.helper.model.TxResult;
import com.bloxbean.cardano.yaci.store.submit.service.OgmiosService;
import com.bloxbean.cardano.yaci.store.submit.service.TxSubmissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
public class BFTxSubmitService {

    private final String submitApiUrl;
    private final ObjectProvider<OgmiosService> ogmiosServiceProvider;
    private final ObjectProvider<TxSubmissionService> txSubmissionServiceProvider;
    private final RestTemplate restTemplate = new RestTemplate();

    public BFTxSubmitService(Environment env,
                             ObjectProvider<OgmiosService> ogmiosServiceProvider,
                             ObjectProvider<TxSubmissionService> txSubmissionServiceProvider) {
        this.submitApiUrl = env.getProperty("store.cardano.submit-api-url");
        this.ogmiosServiceProvider = ogmiosServiceProvider;
        this.txSubmissionServiceProvider = txSubmissionServiceProvider;
    }

    public String submitTx(byte[] cborTx) {
        if (submitApiUrl != null && !submitApiUrl.isBlank()) {
            return submitViaExternalApi(cborTx);
        }

        OgmiosService ogmiosService = ogmiosServiceProvider.getIfAvailable();
        if (ogmiosService != null) {
            return submitViaOgmios(ogmiosService, cborTx);
        }

        TxSubmissionService txSubmissionService = txSubmissionServiceProvider.getIfAvailable();
        if (txSubmissionService != null) {
            return submitViaLocalNode(txSubmissionService, cborTx);
        }

        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "No transaction submission backend configured. Configure one of: store.cardano.submit-api-url, store.cardano.ogmios-url, or store.cardano.n2c-node-socket-path");
    }

    private String submitViaExternalApi(byte[] cborTx) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/cbor");
        HttpEntity<byte[]> entity = new HttpEntity<>(cborTx, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(submitApiUrl, HttpMethod.POST, entity, String.class);
            return response.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new ResponseStatusException(HttpStatus.valueOf(e.getStatusCode().value()), e.getResponseBodyAsString());
        }
    }

    private String submitViaOgmios(OgmiosService ogmiosService, byte[] cborTx) {
        try {
            Result<String> result = ogmiosService.submitTx(cborTx);
            if (result.isSuccessful()) {
                return result.getValue();
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, result.getResponse());
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private String submitViaLocalNode(TxSubmissionService txSubmissionService, byte[] cborTx) {
        TxResult txResult = txSubmissionService.submitTx(TxBodyType.CONWAY, cborTx);
        if (txResult.isAccepted()) {
            return txResult.getTxHash();
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, txResult.getErrorCbor());
    }
}

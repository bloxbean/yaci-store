package com.bloxbean.cardano.yaci.store.blockfrost.util.service;

import com.bloxbean.cardano.yaci.core.util.HexUtil;
import com.bloxbean.cardano.yaci.store.submit.service.TxEvaluationService;
import com.fasterxml.jackson.databind.JsonNode;
import io.vavr.control.Either;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class BFUtilService {

    private final TxEvaluationService txEvaluationService;

    public JsonNode evaluateTx(byte[] cborTx, int version) {
        try {
            Either<JsonNode, JsonNode> result = txEvaluationService.evaluateTx(cborTx, null);
            return formatResult(result, version);
        } catch (Exception e) {
            if (log.isDebugEnabled())
                log.error("Transaction evaluation failed", e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    public JsonNode evaluateTxWithUtxos(String cbor, JsonNode additionalUtxoSet, int version) {
        try {
            byte[] cborTx = decodeCbor(cbor);
            Either<JsonNode, JsonNode> result = txEvaluationService.evaluateTx(cborTx, additionalUtxoSet);
            return formatResult(result, version);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            if (log.isDebugEnabled())
                log.error("Transaction evaluation with utxos failed", e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    private byte[] decodeCbor(String cbor) {
        if (cbor == null || cbor.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cbor field is required");
        }
        String trimmed = cbor.trim();
        if (trimmed.matches("[0-9a-fA-F]+")) {
            return HexUtil.decodeHexString(trimmed);
        }
        return Base64.getDecoder().decode(trimmed);
    }

    private JsonNode formatResult(Either<JsonNode, JsonNode> result, int version) {
        if (result.isRight()) {
            if (version == 6) {
                return result.get();
            }
            return txEvaluationService.transformTxEvaluationSuccessResultToV5BFFormat(result.get());
        } else {
            if (version == 6) {
                return result.getLeft();
            }
            return txEvaluationService.transformTxEvaluationErrorToV5BFFormat(result.getLeft());
        }
    }
}

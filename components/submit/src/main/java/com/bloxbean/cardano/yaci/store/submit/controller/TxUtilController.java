package com.bloxbean.cardano.yaci.store.submit.controller;

import com.bloxbean.cardano.client.api.model.EvaluationResult;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.store.common.util.JsonUtil;
import com.bloxbean.cardano.yaci.store.submit.service.OgmiosService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.adabox.model.base.RawResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Utilities")
@RestController
@RequestMapping("${apiPrefix}/utils/txs")
@RequiredArgsConstructor
@ConditionalOnBean(OgmiosService.class)
@Slf4j
public class TxUtilController {

    private final OgmiosService ogmiosService;
    private final ObjectMapper objectMapper;

    @PostMapping(value = "evaluate", consumes = {MediaType.APPLICATION_CBOR_VALUE})
    public ResponseEntity<String> evaluateTx(@RequestBody String cborTx) {
        try {
            var cborBytes = HexUtil.decodeHexString(cborTx);
            Result<List<EvaluationResult>> result = ogmiosService.evaluateTx(cborBytes);
            if (result.isSuccessful()) {
                return ResponseEntity.accepted()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(transformResultToBFFormat(result.getValue()));
            } else {
                return ResponseEntity.badRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(transformErrorResultToBFFormat(result.getResponse()));
            }
        } catch (WebsocketNotConnectedException ex) {
            return ResponseEntity.badRequest()
                    .body("Ogmios websocket is not connected. " + ogmiosService.getOgmiosUrl());
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }
    }

    private String transformResultToBFFormat(List<EvaluationResult> evaluationResults) {
        RawResponse rawResponse = new RawResponse();
        rawResponse.setType("jsonwsp/response");
        rawResponse.setVersion("1.0");
        rawResponse.setServicename("ogmios");
        rawResponse.setMethodname("EvaluateTx");

        ObjectNode resultNode = objectMapper.createObjectNode();
        ObjectNode evaluationResultsNode = objectMapper.createObjectNode();
        resultNode.set("EvaluationResult", evaluationResultsNode);
        for (EvaluationResult evaluationResult : evaluationResults) {
            ObjectNode cost = objectMapper.createObjectNode();
            cost.put("memory", evaluationResult.getExUnits().getMem());
            cost.put("steps", evaluationResult.getExUnits().getSteps());

            var tagIndex = evaluationResult.getRedeemerTag().name().toLowerCase() + ":" + evaluationResult.getIndex();

            evaluationResultsNode.set(tagIndex, cost);
        }

        rawResponse.setResult(resultNode);

        return JsonUtil.getJson(rawResponse);
    }

    private String transformErrorResultToBFFormat(String error) {
        RawResponse rawResponse = new RawResponse();
        rawResponse.setType("jsonwsp/response");
        rawResponse.setVersion("1.0");
        rawResponse.setServicename("ogmios");
        rawResponse.setMethodname("EvaluateTx");

        ObjectNode resultNode = objectMapper.createObjectNode();

        try {
            var errorNode = JsonUtil.parseJson(error);
            resultNode.set("EvaluationFailure", errorNode);
        } catch (JsonProcessingException e) {
            resultNode.set("EvaluationFailure", new TextNode(error));
        }

        rawResponse.setResult(resultNode);

        return JsonUtil.getJson(rawResponse);
    }
}

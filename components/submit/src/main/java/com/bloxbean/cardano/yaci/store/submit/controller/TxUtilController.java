package com.bloxbean.cardano.yaci.store.submit.controller;

import com.bloxbean.cardano.client.api.model.EvaluationResult;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.yaci.store.common.util.JsonUtil;
import com.bloxbean.cardano.yaci.store.submit.service.OgmiosService;
import lombok.RequiredArgsConstructor;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

//@RestController
//@RequestMapping("${apiPrefix}/utils/txs")
@RequiredArgsConstructor
//@ConditionalOnBean(OgmiosService.class)
//@Slf4j
public class TxUtilController {

    private final OgmiosService ogmiosService;

    @PostMapping(value = "evaluate", consumes = {MediaType.APPLICATION_CBOR_VALUE})
    public ResponseEntity<String> evaluateTx(@RequestBody byte[] cborTx) {
        try {

            Result<List<EvaluationResult>> result = ogmiosService.evaluateTx(cborTx);
            if (result.isSuccessful()) {
                return ResponseEntity.accepted()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(JsonUtil.getJson(result.getValue()));
            } else {
                return ResponseEntity.badRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(result.getResponse());
            }
        } catch (WebsocketNotConnectedException ex) {
            return ResponseEntity.badRequest()
                    .body("Ogmios websocket is not connected. " + ogmiosService.getOgmiosUrl());
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }
    }
}

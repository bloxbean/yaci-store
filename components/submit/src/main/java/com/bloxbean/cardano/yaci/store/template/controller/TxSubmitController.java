package com.bloxbean.cardano.yaci.store.template.controller;

import com.bloxbean.cardano.yaci.core.common.TxBodyType;
import com.bloxbean.cardano.yaci.core.util.HexUtil;
import com.bloxbean.cardano.yaci.helper.LocalClientProvider;
import com.bloxbean.cardano.yaci.helper.model.TxResult;
import com.bloxbean.cardano.yaci.store.template.service.TxSubmissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${apiPrefix}")
@RequiredArgsConstructor
@ConditionalOnBean(LocalClientProvider.class)
@Slf4j
public class TxSubmitController {

    private final TxSubmissionService txSubmissionService;

    @PostMapping(value = "/submit/tx", consumes = {MediaType.APPLICATION_CBOR_VALUE},
            produces = MediaType.APPLICATION_JSON_VALUE)
    public TxResult submitTx(@RequestBody byte[] txBytes) {
        TxResult txResult = txSubmissionService.submitTx(TxBodyType.BABBAGE, txBytes);
        if (log.isDebugEnabled())
            log.debug(String.valueOf(txResult));
        return txResult;
    }

    @PostMapping(value = "/submit/tx", consumes = {MediaType.TEXT_PLAIN_VALUE},
            produces = MediaType.APPLICATION_JSON_VALUE)
    public TxResult submitTx(@RequestBody String txBytesHex) {
        byte[] txBytes = HexUtil.decodeHexString(txBytesHex);
        TxResult txResult = txSubmissionService.submitTx(TxBodyType.BABBAGE, txBytes);
        if (log.isDebugEnabled())
            log.debug(String.valueOf(txResult));
        return txResult;
    }
}

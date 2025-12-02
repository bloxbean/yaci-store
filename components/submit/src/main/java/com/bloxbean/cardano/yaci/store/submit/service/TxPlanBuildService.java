package com.bloxbean.cardano.yaci.store.submit.service;

import com.bloxbean.cardano.client.exception.CborSerializationException;
import com.bloxbean.cardano.client.function.exception.TxBuildException;
import com.bloxbean.cardano.client.quicktx.QuickTxBuilder;
import com.bloxbean.cardano.client.quicktx.signing.SignerRegistry;
import com.bloxbean.cardano.client.quicktx.serialization.TxPlan;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.store.submit.service.exception.TxPlanBuildException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * Service to build unsigned transactions from YAML TxPlan definitions using QuickTx.
 */
@Service
@ConditionalOnBean(QuickTxBuilder.class)
@RequiredArgsConstructor
@Slf4j
public class TxPlanBuildService {

    private final QuickTxBuilder quickTxBuilder;
    private final Optional<SignerRegistry> signerRegistry;

    public TxPlanBuildResult buildFromYaml(String yaml) throws CborSerializationException {
        if (!StringUtils.hasText(yaml)) {
            throw new TxPlanBuildException("TxPlan YAML must not be empty");
        }

        TxPlan plan = TxPlan.from(yaml);
        QuickTxBuilder.TxContext context = signerRegistry
                .map(registry -> quickTxBuilder.compose(plan, registry))
                .orElseGet(() -> quickTxBuilder.compose(plan));
        var transaction = context.build();
        String cborHex = HexUtil.encodeHexString(transaction.serialize());
        return new TxPlanBuildResult(cborHex);
    }

    public record TxPlanBuildResult(String txBodyCbor) {}
}

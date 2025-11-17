package com.bloxbean.cardano.yaci.store.submit.service;

import com.bloxbean.cardano.client.exception.CborSerializationException;
import com.bloxbean.cardano.client.quicktx.AbstractTx;
import com.bloxbean.cardano.client.quicktx.QuickTxBuilder;
import com.bloxbean.cardano.client.quicktx.serialization.TxPlan;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.store.submit.service.exception.TxPlanBuildException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

/**
 * Service to build unsigned transactions from YAML TxPlan definitions using QuickTx.
 */
@Service
@ConditionalOnBean(QuickTxBuilder.class)
@RequiredArgsConstructor
@Slf4j
public class TxPlanBuildService {

    private final QuickTxBuilder quickTxBuilder;

    public TxPlanBuildResult buildFromYaml(String yaml) throws CborSerializationException {
        if (!StringUtils.hasText(yaml)) {
            throw new TxPlanBuildException("TxPlan YAML must not be empty");
        }

        TxPlan plan = TxPlan.from(yaml);
        List<AbstractTx<?>> txs = plan.getTxs();
        if (CollectionUtils.isEmpty(txs)) {
            throw new TxPlanBuildException("TxPlan does not contain any transactions");
        }

        QuickTxBuilder.TxContext context = quickTxBuilder.compose(txs.toArray(new AbstractTx[0]));
        applyContext(plan, context);

        var transaction = context.build();
        String cborHex = HexUtil.encodeHexString(transaction.serialize());

        return new TxPlanBuildResult(cborHex);
    }

    private void applyContext(TxPlan plan, QuickTxBuilder.TxContext context) {
        if (StringUtils.hasText(plan.getFeePayer())) {
            context.feePayer(plan.getFeePayer());
        }

        if (StringUtils.hasText(plan.getCollateralPayer())) {
            context.collateralPayer(plan.getCollateralPayer());
        }

        if (StringUtils.hasText(plan.getFeePayerRef()) || StringUtils.hasText(plan.getCollateralPayerRef())) {
            throw new TxPlanBuildException("fee_payer_ref / collateral_payer_ref are not supported yet. Provide concrete addresses.");
        }

        if (!CollectionUtils.isEmpty(plan.getSignerRefs())) {
            throw new TxPlanBuildException("Signer references are not supported yet. Provide required signers as credential hex.");
        }

        if (!CollectionUtils.isEmpty(plan.getRequiredSigners())) {
            context.withRequiredSigners(convertHexToBytes(plan.getRequiredSigners()));
        }

        if (plan.getValidFromSlot() != null && plan.getValidFromSlot() > 0) {
            context.validFrom(plan.getValidFromSlot());
        }

        if (plan.getValidToSlot() != null && plan.getValidToSlot() > 0) {
            context.validTo(plan.getValidToSlot());
        }
    }

    private byte[][] convertHexToBytes(Set<String> hexStrings) {
        return hexStrings.stream()
                .filter(StringUtils::hasText)
                .map(HexUtil::decodeHexString)
                .toArray(byte[][]::new);
    }

    public record TxPlanBuildResult(String txBodyCbor) {}
}


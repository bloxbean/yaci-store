package com.bloxbean.cardano.yaci.store.adapot.processor;

import com.bloxbean.cardano.yaci.store.transaction.domain.event.TxnEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class FeePotProcessor {

    private List<BigInteger> feesCache = Collections.synchronizedList(new ArrayList<>());

    @EventListener
    @Transactional
    public void handleFeeInTxsOfBlock(TxnEvent txnEvent) {
        var transactions = txnEvent.getTxnList();
        if (transactions == null || transactions.isEmpty())
            return;

        BigInteger blockFee = BigInteger.ZERO;
        for (var transaction: transactions) {
            var txFee = transaction.getFee();
            if (txFee == null) {
                log.error("Fee not found for transaction : {}. Something wrong !!!", transaction.getTxHash());
                continue;
            }

            blockFee = blockFee.add(txFee);
        }

        feesCache.add(blockFee);
    }

    public BigInteger getTotalFeeInBatch() {
        return feesCache.stream()
                .reduce(BigInteger::add)
                .orElse(BigInteger.ZERO);
    }

    public void reset() {
        feesCache.clear();
    }

}

package com.bloxbean.cardano.yaci.store.transaction.processor;

import com.bloxbean.cardano.yaci.helper.model.Transaction;
import com.bloxbean.cardano.yaci.store.client.utxo.UtxoClient;
import com.bloxbean.cardano.yaci.store.common.aspect.EnableIf;
import com.bloxbean.cardano.yaci.store.common.util.TransactionFeeUtil;
import com.bloxbean.cardano.yaci.store.transaction.TransactionStoreConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.math.BigInteger;

/**
 * Helps to resolve fee. This is useful to resolve fee from collateral for invalid transactions
 */
@Component
@RequiredArgsConstructor
@EnableIf(TransactionStoreConfiguration.STORE_TRANSACTION_ENABLED)
@Slf4j
public class FeeResolver {
    private final UtxoClient utxoClient;

    /**
     * Returns null when the collateral fee can't be resolved, e.g. missing collateral UTxOs or a failed lookup.
     */
    public BigInteger resolveFee(Transaction transaction) {
        try {
            return TransactionFeeUtil.resolveFee(transaction.getBody(), transaction.isInvalid(), utxoClient::getUtxosByIds);
        } catch (RestClientException e) {
            log.warn("Collateral UTxO lookup failed for transaction {}: {}", transaction.getBody().getTxHash(), e.getMessage());
            return null;
        }
    }
}

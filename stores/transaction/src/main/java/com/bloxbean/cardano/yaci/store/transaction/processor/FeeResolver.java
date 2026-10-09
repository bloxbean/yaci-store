package com.bloxbean.cardano.yaci.store.transaction.processor;

import com.bloxbean.cardano.yaci.helper.model.Transaction;
import com.bloxbean.cardano.yaci.store.client.utxo.UtxoClient;
import com.bloxbean.cardano.yaci.store.common.aspect.EnableIf;
import com.bloxbean.cardano.yaci.store.common.util.TransactionFeeUtil;
import com.bloxbean.cardano.yaci.store.transaction.TransactionStoreConfiguration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigInteger;

/**
 * Helps to resolve fee. This is useful to resolve fee from collateral for invalid transactions
 */
@Component
@RequiredArgsConstructor
@EnableIf(TransactionStoreConfiguration.STORE_TRANSACTION_ENABLED)
public class FeeResolver {
    private final UtxoClient utxoClient;

    public BigInteger resolveFee(Transaction transaction) {
        return TransactionFeeUtil.resolveFee(transaction.getBody(), transaction.isInvalid(), utxoClient::getUtxosByIds);
    }
}

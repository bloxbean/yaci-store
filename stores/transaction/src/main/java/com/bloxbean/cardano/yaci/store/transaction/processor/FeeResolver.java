package com.bloxbean.cardano.yaci.store.transaction.processor;

import com.bloxbean.cardano.yaci.helper.model.Transaction;
import com.bloxbean.cardano.yaci.store.client.utxo.UtxoClient;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigInteger;

/**
 * Helps to resolve fee. This is useful to resolve fee from collateral for invalid transactions
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FeeResolver {
    private final UtxoClient utxoClient;

    public BigInteger resolveFee(Transaction transaction) {
        if (!transaction.isInvalid())
            return transaction.getBody().getFee();

        //For invalid transactions, fee is from collateral
        //For Babbage, get it from total collateral
        if (transaction.getBody().getTotalCollateral() != null && transaction.getBody().getTotalCollateral().compareTo(BigInteger.ZERO) > 0)
            return transaction.getBody().getTotalCollateral();
        else {
            //For Alonzo, get it from collateral inputs
            var collateralInputs = transaction.getBody().getCollateralInputs();
            if (collateralInputs != null && collateralInputs.size() > 0) {
                //Get collateral utxos and set the fee
                var collateralUtxoKeys = collateralInputs.stream()
                        .map(input -> new UtxoKey(input.getTransactionId(), input.getIndex()))
                        .toList();
                var collateralUtxos = utxoClient.getUtxosByIds(collateralUtxoKeys);

                if (collateralUtxos != null && collateralUtxos.size() != collateralUtxoKeys.size()) {
                    log.error("Collateral utxos not found for transaction : {}", transaction.getBody().getTxHash());
                    return null;
                }

                return collateralUtxos.stream()
                        .map(utxo -> utxo.getLovelaceAmount())
                        .reduce(BigInteger.ZERO, BigInteger::add);
            }
        }

        return null;
    }

}

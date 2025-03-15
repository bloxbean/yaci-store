package com.bloxbean.cardano.yaci.store.transaction.processor;

import com.bloxbean.cardano.yaci.helper.model.Transaction;
import com.bloxbean.cardano.yaci.store.client.utxo.UtxoClient;
import com.bloxbean.cardano.yaci.store.common.aspect.EnableIf;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import com.bloxbean.cardano.yaci.store.transaction.TransactionStoreConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigInteger;

import static com.bloxbean.cardano.yaci.core.util.Constants.LOVELACE;

/**
 * Helps to resolve fee. This is useful to resolve fee from collateral for invalid transactions
 */
@Component
@RequiredArgsConstructor
@EnableIf(TransactionStoreConfiguration.STORE_TRANSACTION_ENABLED)
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
            //For Alonzo or when totalCollateral is null, get it from collateral inputs and output if exists
            var collateralInputs = transaction.getBody().getCollateralInputs();
            var collateralReturn = transaction.getBody().getCollateralReturn();

            var totalCollateralInput = BigInteger.ZERO;
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

                totalCollateralInput = collateralUtxos.stream()
                        .map(utxo -> utxo.getLovelaceAmount())
                        .reduce(BigInteger.ZERO, BigInteger::add);
            }

            var totalCollateralOutput = BigInteger.ZERO;
            if (collateralReturn != null && collateralReturn.getAmounts() != null) {
                totalCollateralOutput = collateralReturn.getAmounts().stream()
                        .filter(amount -> amount.getUnit().equals(LOVELACE))
                        .findFirst()
                        .map(amount -> amount.getQuantity())
                        .orElse(BigInteger.ZERO);
            }

            return totalCollateralInput.subtract(totalCollateralOutput);

        }
    }

}

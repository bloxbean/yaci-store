package com.bloxbean.cardano.yaci.store.common.util;

import com.bloxbean.cardano.yaci.core.model.TransactionBody;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.util.List;
import java.util.function.Function;

import static com.bloxbean.cardano.yaci.core.util.Constants.LOVELACE;

/** Calculates the ledger-applied fee; returns null when collateral inputs are not yet available. */
@Slf4j
public final class TransactionFeeUtil {
    private TransactionFeeUtil() {
    }

    public static BigInteger resolveFee(TransactionBody body, boolean invalid,
                                        Function<List<UtxoKey>, List<AddressUtxo>> utxoLookup) {
        if (!invalid)
            return body.getFee();

        //For invalid transactions, fee is from collateral
        //For Babbage, get it from total collateral
        if (body.getTotalCollateral() != null && body.getTotalCollateral().compareTo(BigInteger.ZERO) > 0)
            return body.getTotalCollateral();
        else {
            //For Alonzo or when totalCollateral is null, get it from collateral inputs and output if exists
            var collateralInputs = body.getCollateralInputs();
            var collateralReturn = body.getCollateralReturn();

            var totalCollateralInput = BigInteger.ZERO;
            if (collateralInputs != null && collateralInputs.size() > 0) {
                //Get collateral utxos and set the fee
                var collateralUtxoKeys = collateralInputs.stream()
                        .map(input -> new UtxoKey(input.getTransactionId(), input.getIndex()))
                        .toList();
                var collateralUtxos = utxoLookup.apply(collateralUtxoKeys);

                if (collateralUtxos == null || collateralUtxos.size() != collateralUtxoKeys.size()) {
                    log.debug("Collateral utxos not found for transaction : {}", body.getTxHash());
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

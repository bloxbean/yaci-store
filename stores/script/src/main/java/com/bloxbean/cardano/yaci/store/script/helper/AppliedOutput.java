package com.bloxbean.cardano.yaci.store.script.helper;

import com.bloxbean.cardano.yaci.core.model.TransactionOutput;
import com.bloxbean.cardano.yaci.helper.model.Transaction;

import java.util.stream.IntStream;
import java.util.stream.Stream;

public record AppliedOutput(int index, TransactionOutput output) {
    public static Stream<AppliedOutput> from(Transaction transaction) {
        var outputs = transaction.getBody().getOutputs();
        if (transaction.isInvalid()) {
            var collateralReturn = transaction.getBody().getCollateralReturn();
            // The collateral return follows the ordinary outputs in the transaction's output indexes.
            return collateralReturn == null ? Stream.empty()
                    : Stream.of(new AppliedOutput(outputs.size(), collateralReturn));
        }
        return IntStream.range(0, outputs.size())
                .mapToObj(index -> new AppliedOutput(index, outputs.get(index)));
    }
}

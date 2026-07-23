package com.bloxbean.cardano.yaci.store.blockfrost.transaction.storage.impl.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TxUtxosRaw {
    private boolean txInvalid;
    private List<TxInputRaw> inputs;
    private List<TxOutputRaw> outputs;
    /** Raw JSON from TRANSACTION.COLLATERAL_RETURN (output index reference) */
    private String collateralReturnRefJson;
    /** Raw JSON from TRANSACTION.COLLATERAL_RETURN_JSON (output data) */
    private String collateralReturnDataJson;
}

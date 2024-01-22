package com.bloxbean.cardano.yaci.store.epoch.processor;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import lombok.NonNull;

import java.math.BigInteger;

//Additional Era change specific rules
class PPEraChangeRules {

    /**
     * Apply era change specific rules
     * @param newEra
     * @param prevEra
     * @param protocolParams
     */
    public void apply(@NonNull Era newEra, Era prevEra, ProtocolParams protocolParams) {
        if (newEra != prevEra) {
            if (newEra == Era.Alonzo) {
                //https://cips.cardano.org/cip/CIP-28/
                //minUTxOValue is no longer used. It is replaced by lovelacePerUTxOWord
                protocolParams.setMinUtxo(null);
            } else if (newEra == Era.Babbage) {
                //Removed
                protocolParams.setDecentralisationParam(null);
                protocolParams.setExtraEntropy(null);

                //Translation from the Alonzo era to the Babbage era
                //https://cips.cardano.org/cip/CIP-55/
                var utxoPerBytes = protocolParams.getAdaPerUtxoByte().divide(BigInteger.valueOf(8));
                protocolParams.setAdaPerUtxoByte(utxoPerBytes);
            } else if (newEra == Era.Conway) {
                //TODO -- Conway era specific rules
            }
        }
    }
}

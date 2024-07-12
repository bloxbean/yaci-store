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
                applyAlonzoRules(protocolParams);
            } else if (newEra == Era.Babbage) {
                if (prevEra == null){ //custom network starting directly from Babbage era
                    applyAlonzoRules(protocolParams);
                }
                applyBabbageRules(protocolParams);
            } else if (newEra == Era.Conway) {
                if (prevEra == null) { //custom network starting directly from Conway era
                    applyAlonzoRules(protocolParams);
                    applyBabbageRules(protocolParams);
                }

                //TODO: Apply Conway specific new rules
            }
        }
    }

    private static void applyAlonzoRules(ProtocolParams protocolParams) {
        //https://cips.cardano.org/cip/CIP-28/
        //minUTxOValue is no longer used. It is replaced by lovelacePerUTxOWord
        protocolParams.setMinUtxo(null);
    }

    private static void applyBabbageRules(ProtocolParams protocolParams) {
        //Removed
        protocolParams.setDecentralisationParam(null);
        protocolParams.setExtraEntropy(null);

        //Translation from the Alonzo era to the Babbage era
        //https://cips.cardano.org/cip/CIP-55/
        var utxoPerBytes = protocolParams.getAdaPerUtxoByte().divide(BigInteger.valueOf(8));
        protocolParams.setAdaPerUtxoByte(utxoPerBytes);
    }
}

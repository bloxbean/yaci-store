package com.bloxbean.cardano.yaci.store.events.domain;

import com.bloxbean.cardano.yaci.core.model.Datum;
import com.bloxbean.cardano.yaci.core.model.NativeScript;
import com.bloxbean.cardano.yaci.core.model.PlutusScript;
import com.bloxbean.cardano.yaci.core.model.Redeemer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TxScripts {
    private String txHash;
    private List<PlutusScript> plutusV1Scripts;
    private List<Datum> datums;
    private List<Redeemer> redeemers;
    private List<PlutusScript> plutusV2Scripts;
    private List<PlutusScript> plutusV3Scripts;
    private List<NativeScript> nativeScripts;
    //private List<PlutusScript> scriptRef;
    //Dijkstra era (witness set key 8); null before
    private List<PlutusScript> plutusV4Scripts;

    public TxScripts(String txHash, List<PlutusScript> plutusV1Scripts, List<Datum> datums, List<Redeemer> redeemers,
                     List<PlutusScript> plutusV2Scripts, List<PlutusScript> plutusV3Scripts,
                     List<NativeScript> nativeScripts) {
        this(txHash, plutusV1Scripts, datums, redeemers, plutusV2Scripts, plutusV3Scripts, nativeScripts, null);
    }
}

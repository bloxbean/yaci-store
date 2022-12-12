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
    private List<NativeScript> nativeScripts;
    //private List<PlutusScript> scriptRef;
}

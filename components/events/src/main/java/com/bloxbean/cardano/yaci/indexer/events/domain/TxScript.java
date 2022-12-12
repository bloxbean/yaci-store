package com.bloxbean.cardano.yaci.indexer.events.domain;

import com.bloxbean.cardano.yaci.core.model.Datum;
import com.bloxbean.cardano.yaci.core.model.NativeScript;
import com.bloxbean.cardano.yaci.core.model.PlutusScript;
import com.bloxbean.cardano.yaci.core.model.Redeemer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TxScript {
    private String txHash;
    private String scriptHash;
    private Redeemer redeemer;
    private Datum datum;
    private PlutusScript plutusScript;
    private NativeScript nativeScript;
}

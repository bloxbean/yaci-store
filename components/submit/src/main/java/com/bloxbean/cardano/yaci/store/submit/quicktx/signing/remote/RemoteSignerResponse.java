package com.bloxbean.cardano.yaci.store.submit.quicktx.signing.remote;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class RemoteSignerResponse {
    byte[] signature;
    byte[] verificationKey;
}

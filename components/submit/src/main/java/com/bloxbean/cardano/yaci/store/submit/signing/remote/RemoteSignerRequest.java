package com.bloxbean.cardano.yaci.store.submit.signing.remote;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RemoteSignerRequest {
    String ref;
    String keyId;
    String scope;
    byte[] txBody;
    String endpoint;
    String authToken;
    String hostPublicKey;
    String verificationKey;
    String address;
    Integer timeoutMs;
}

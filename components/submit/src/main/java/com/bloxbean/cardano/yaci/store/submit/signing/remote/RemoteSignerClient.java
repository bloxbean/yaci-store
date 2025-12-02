package com.bloxbean.cardano.yaci.store.submit.signing.remote;

public interface RemoteSignerClient {
    RemoteSignerResponse sign(RemoteSignerRequest request) throws RemoteSignerException;
}

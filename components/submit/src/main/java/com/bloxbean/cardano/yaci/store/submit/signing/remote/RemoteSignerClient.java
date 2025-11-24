package com.bloxbean.cardano.yaci.store.submit.signing.remote;

public interface RemoteSignerClient {

    /**
     * Delegate signing to a remote signer.
     *
     * @param request request containing tx body and routing metadata
     * @return signature + verification key to add as a witness
     */
    RemoteSignerResponse sign(RemoteSignerRequest request) throws RemoteSignerException;
}

package com.bloxbean.cardano.yaci.store.common.util;

import com.bloxbean.cardano.client.address.Credential;
import com.bloxbean.cardano.yaci.core.model.certs.StakeCredType;
import com.bloxbean.cardano.yaci.core.model.certs.StakeCredential;

public class CCLUtil {

    public static Credential toCCLCredential(StakeCredential credential) {
        if (credential.getType() == StakeCredType.ADDR_KEYHASH) {
            return Credential.fromKey(credential.getHash());
        } else if (credential.getType() == StakeCredType.SCRIPTHASH) {
            return Credential.fromScript(credential.getHash());
        } else {
            throw new IllegalArgumentException("Invalid credential type");
        }
    }
}

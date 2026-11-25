package com.bloxbean.cardano.yaci.store.epochnonce.client;

import com.bloxbean.cardano.yaci.store.client.epoch.EpochNonceClient;
import com.bloxbean.cardano.yaci.store.epochnonce.domain.EpochNonce;
import com.bloxbean.cardano.yaci.store.epochnonce.storage.EpochNonceStorageReader;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("epochNonceClient")
@Primary
public class EpochNonceClientImpl implements EpochNonceClient {
    private final EpochNonceStorageReader epochNonceStorageReader;

    public EpochNonceClientImpl(EpochNonceStorageReader epochNonceStorageReader) {
        this.epochNonceStorageReader = epochNonceStorageReader;
    }

    @Override
    public Optional<String> getEpochNonce(int epoch) {
        return epochNonceStorageReader.findByEpoch(epoch)
                .map(EpochNonce::getNonce);
    }
}

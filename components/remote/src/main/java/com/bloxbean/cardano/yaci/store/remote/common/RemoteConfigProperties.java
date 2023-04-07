package com.bloxbean.cardano.yaci.store.remote.common;

import lombok.Data;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
@Data
public class RemoteConfigProperties {
    private final String remoteId;

    public RemoteConfigProperties() {
        remoteId = UUID.randomUUID().toString();
    }
}

package com.bloxbean.cardano.yaci.store.remote;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Data
public class RemoteProperties {
    private boolean publisherEnabled = false;
    private boolean consumerEnabled = false;
    private List<String> publisherEvents = new ArrayList<>();
}

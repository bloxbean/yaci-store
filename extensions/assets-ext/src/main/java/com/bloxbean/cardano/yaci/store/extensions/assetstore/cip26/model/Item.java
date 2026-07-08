package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.model;

import java.util.List;

public record Item(Integer sequenceNumber, String value, List<Signature> signatures) {
}

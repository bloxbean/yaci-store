package com.bloxbean.cardano.yaci.store.extensions.assetstore.api.model.v2;

import java.util.List;

public record Response(Subject subject, List<String> queryPriority) {
}

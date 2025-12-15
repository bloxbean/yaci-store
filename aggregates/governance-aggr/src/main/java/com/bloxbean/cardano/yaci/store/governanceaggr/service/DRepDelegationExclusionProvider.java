package com.bloxbean.cardano.yaci.store.governanceaggr.service;

import com.bloxbean.cardano.yaci.store.governanceaggr.domain.DRepDelegationExclusion;

import java.util.List;

public interface DRepDelegationExclusionProvider {
    List<DRepDelegationExclusion> getExclusionsForNetwork(long protocolMagic);
}


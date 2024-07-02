package com.bloxbean.cardano.yaci.store.governance.storage.impl.projection;

public interface DelegationVoteProjection {
  String getDrepHash();

  String getAddress();

  String getTxHash();
}

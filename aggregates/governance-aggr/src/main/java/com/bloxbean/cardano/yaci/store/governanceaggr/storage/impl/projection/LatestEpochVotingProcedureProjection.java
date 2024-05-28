package com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.projection;

public interface LatestEpochVotingProcedureProjection {
  Long getEpoch();

  String getVoterHash();
}

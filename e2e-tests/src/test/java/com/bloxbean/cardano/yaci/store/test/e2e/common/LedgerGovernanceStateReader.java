package com.bloxbean.cardano.yaci.store.test.e2e.common;

import com.bloxbean.cardano.yaci.core.model.certs.StakePoolId;
import com.bloxbean.cardano.yaci.core.model.governance.Drep;
import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

public interface LedgerGovernanceStateReader {
    ProposalLedgerSnapshot fetchProposalState(GovActionId govActionId);

    BigInteger fetchTreasury();

    Map<Drep, BigInteger> fetchDRepStakeDistribution(List<com.bloxbean.cardano.client.transaction.spec.governance.DRep> dReps);

    Map<StakePoolId, BigInteger> fetchSPOStakeDistribution(List<String> poolKeyHashes);
}

package com.bloxbean.cardano.yaci.store.test.e2e.common;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.core.protocol.localstate.api.Era;
import com.bloxbean.cardano.yaci.core.protocol.localstate.api.EraQuery;
import com.bloxbean.cardano.yaci.core.protocol.localstate.api.QueryResult;
import com.bloxbean.cardano.yaci.core.protocol.localstate.queries.DRepStakeDistributionQuery;
import com.bloxbean.cardano.yaci.core.protocol.localstate.queries.DRepStakeDistributionQueryResult;
import com.bloxbean.cardano.yaci.core.protocol.localstate.queries.GovStateQuery;
import com.bloxbean.cardano.yaci.core.protocol.localstate.queries.GovStateQueryResult;
import com.bloxbean.cardano.yaci.core.protocol.localstate.queries.SPOStakeDistributionQuery;
import com.bloxbean.cardano.yaci.core.protocol.localstate.queries.SPOStakeDistributionQueryResult;
import com.bloxbean.cardano.yaci.core.protocol.localstate.queries.model.Proposal;
import com.bloxbean.cardano.yaci.core.protocol.localstate.queries.model.RatifyState;
import com.bloxbean.cardano.yaci.helper.LocalClientProvider;

import java.math.BigInteger;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class DevKitLedgerGovernanceStateReader implements LedgerGovernanceStateReader {
    private static final Duration DEFAULT_QUERY_TIMEOUT = Duration.ofSeconds(20);

    private final String nodeSocketPath;
    private final String n2cHost;
    private final int n2cPort;
    private final long protocolMagic;
    private final Duration queryTimeout;
    private final Set<GovActionId> observedGovActionIds = ConcurrentHashMap.newKeySet();

    public DevKitLedgerGovernanceStateReader(String n2cHost, int n2cPort, long protocolMagic) {
        this(null, n2cHost, n2cPort, protocolMagic, DEFAULT_QUERY_TIMEOUT);
    }

    public DevKitLedgerGovernanceStateReader(String nodeSocketPath,
                                             String n2cHost,
                                             int n2cPort,
                                             long protocolMagic,
                                             Duration queryTimeout) {
        this.nodeSocketPath = blankToNull(nodeSocketPath);
        this.n2cHost = blankToNull(n2cHost);
        this.n2cPort = n2cPort;
        this.protocolMagic = protocolMagic;
        this.queryTimeout = queryTimeout == null ? DEFAULT_QUERY_TIMEOUT : queryTimeout;
    }

    @Override
    public ProposalLedgerSnapshot fetchProposalState(GovActionId govActionId) {
        GovStateQueryResult govState = executeLocalStateQuery(new GovStateQuery(Era.Conway));
        RatifyState nextRatifyState = govState.getNextRatifyState();

        Proposal activeProposal = findProposal(safeProposals(govState.getProposals()), govActionId);
        Proposal enactedProposal = nextRatifyState == null
                ? null
                : findProposal(safeProposals(nextRatifyState.getEnactedGovActions()), govActionId);
        boolean expired = nextRatifyState != null
                && safeGovActionIds(nextRatifyState.getExpiredGovActions()).contains(govActionId);

        boolean active = activeProposal != null;
        boolean enacted = enactedProposal != null;
        boolean observedBefore = observedGovActionIds.contains(govActionId);
        if (active || enacted || expired) {
            observedGovActionIds.add(govActionId);
        }

        boolean removed = observedBefore && !active;
        Proposal sourceProposal = activeProposal != null ? activeProposal : enactedProposal;

        return new ProposalLedgerSnapshot(
                govActionId,
                classify(active, enacted, expired, removed),
                active,
                enacted,
                expired,
                removed,
                nextRatifyState != null && Boolean.TRUE.equals(nextRatifyState.getRatificationDelayed()),
                sourceProposal == null ? null : sourceProposal.getProposedIn(),
                sourceProposal == null ? null : sourceProposal.getExpiredAfter());
    }

    @Override
    public Map<com.bloxbean.cardano.yaci.core.model.governance.Drep, BigInteger> fetchDRepStakeDistribution(
            List<com.bloxbean.cardano.client.transaction.spec.governance.DRep> dReps) {
        DRepStakeDistributionQueryResult result = executeLocalStateQuery(new DRepStakeDistributionQuery(dReps));
        return result.getDRepStakeMap();
    }

    @Override
    public Map<com.bloxbean.cardano.yaci.core.model.certs.StakePoolId, BigInteger> fetchSPOStakeDistribution(List<String> poolKeyHashes) {
        SPOStakeDistributionQueryResult result = executeLocalStateQuery(new SPOStakeDistributionQuery(poolKeyHashes));
        return result.getSpoStakeMap();
    }

    @SuppressWarnings("unchecked")
    private <T extends QueryResult> T executeLocalStateQuery(EraQuery<T> query) {
        LocalClientProvider provider = createProvider();
        try {
            provider.suppressConnectionInfoLog(true);
            provider.start();

            var queryClient = provider.getLocalStateQueryClient();
            try {
                queryClient.release().block(Duration.ofSeconds(5));
            } catch (Exception ignored) {
                // A fresh connection may not have an acquired state yet.
            }

            try {
                queryClient.acquire().block(Duration.ofSeconds(5));
            } catch (Exception ignored) {
                // The query path below will fail with context if acquire was actually required.
            }

            T result = (T) queryClient
                    .executeQuery(query)
                    .block(queryTimeout);
            if (result == null) {
                throw new IllegalStateException("DevKit ledger state reader returned no local-state result within "
                        + queryTimeout.toSeconds() + "s");
            }
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to query DevKit governance state via N2C "
                    + connectionDescription()
                    + ". Ensure DevKit exposes n2c socat TCP or node socket access and protocol magic "
                    + protocolMagic + " is correct.", e);
        } finally {
            provider.shutdown();
        }
    }

    private LocalClientProvider createProvider() {
        if (nodeSocketPath != null) {
            return new LocalClientProvider(nodeSocketPath, protocolMagic);
        }

        if (n2cHost == null) {
            throw new IllegalStateException("DevKit ledger state reader requires either nodeSocketPath or n2cHost");
        }

        return new LocalClientProvider(n2cHost, n2cPort, protocolMagic);
    }

    private ProposalLedgerState classify(boolean active, boolean enacted, boolean expired, boolean removed) {
        if (enacted) {
            return ProposalLedgerState.LEDGER_RATIFIED;
        }

        if (expired) {
            return ProposalLedgerState.LEDGER_EXPIRED;
        }

        if (active) {
            return ProposalLedgerState.LEDGER_ACTIVE;
        }

        if (removed) {
            return ProposalLedgerState.LEDGER_REMOVED;
        }

        return ProposalLedgerState.LEDGER_UNKNOWN;
    }

    private Proposal findProposal(List<Proposal> proposals, GovActionId govActionId) {
        return proposals.stream()
                .filter(proposal -> govActionId.equals(proposal.getGovActionId()))
                .findFirst()
                .orElse(null);
    }

    private List<Proposal> safeProposals(List<Proposal> proposals) {
        return proposals == null ? Collections.emptyList() : proposals;
    }

    private List<GovActionId> safeGovActionIds(List<GovActionId> govActionIds) {
        return govActionIds == null ? Collections.emptyList() : govActionIds;
    }

    private String connectionDescription() {
        if (nodeSocketPath != null) {
            return "node-socket=" + nodeSocketPath;
        }

        return "host=" + n2cHost + ", port=" + n2cPort;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}

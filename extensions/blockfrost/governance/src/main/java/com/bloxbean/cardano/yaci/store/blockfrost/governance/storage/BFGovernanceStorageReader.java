package com.bloxbean.cardano.yaci.store.blockfrost.governance.storage;

import com.bloxbean.cardano.yaci.store.blockfrost.governance.storage.impl.model.BFDRepDelegator;
import com.bloxbean.cardano.yaci.store.blockfrost.governance.storage.impl.model.BFDRep;
import com.bloxbean.cardano.yaci.store.blockfrost.governance.storage.impl.model.BFProposal;
import com.bloxbean.cardano.yaci.store.blockfrost.governance.util.BFDRepIdentity;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.governance.domain.DRepRegistration;
import com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure;

import java.util.List;
import java.util.Optional;

public interface BFGovernanceStorageReader {

    // ── DRep ─────────────────────────────────────────────────────────────────

    /** Paginated list of all unique DReps enriched with current list response fields. */
    List<BFDRep> findAllDReps(int page, int count, Order order);

    /**
     * Distinct CIP-129 {@code drep_id} values stored for a raw 28-byte hash.
     * Used to reject ambiguous hex path parameters when key and script twins coexist.
     */
    List<String> findDRepIdsByHash(String drepHash);

    /** Latest drep row for the given CIP-129 identity, enriched with voting power and script flag. */
    Optional<BFDRep> findDRepById(BFDRepIdentity identity);

    /** Delegators for a DRep with their total unspent lovelace. */
    List<BFDRepDelegator> findDRepDelegators(BFDRepIdentity identity, int page, int count, Order order);

    /** All drep_registration cert rows for the given DRep identity. */
    List<DRepRegistration> findDRepUpdates(BFDRepIdentity identity, int page, int count, Order order);

    /** Voting procedure rows for the given DRep voter identity. */
    List<VotingProcedure> findDRepVotes(BFDRepIdentity identity, int page, int count, Order order);

    /** Most recent drep_registration row that has an anchor URL. */
    Optional<DRepRegistration> findDRepMetadata(BFDRepIdentity identity);

    // ── Proposals ────────────────────────────────────────────────────────────

    /** Paginated list of all governance proposals. */
    List<BFProposal> findAllProposals(int page, int count, Order order);

    /** Single proposal identified by transaction hash and proposal index. */
    Optional<BFProposal> findProposalByTxHashAndIndex(String txHash, int index);

    /**
     * Returns the proposal only if it is a PARAMETER_CHANGE_ACTION type.
     * Returns empty if not found or wrong type.
     */
    Optional<BFProposal> findParameterChangeProposal(String txHash, int index);

    /**
     * Returns true if the proposal at (txHash, index) is TREASURY_WITHDRAWALS_ACTION.
     */
    boolean isWithdrawalProposal(String txHash, int index);

    /**
     * Treasury withdrawal entries for the given proposal (tx_hash + gov_action_index).
     * Returns address + lovelace amount pairs.
     */
    List<BFDRepDelegator> findProposalWithdrawals(String txHash, int index, int page, int count, Order order);

    /** Voting procedure rows for the given proposal. */
    List<VotingProcedure> findProposalVotes(String txHash, int index, int page, int count, Order order);

    /**
     * Returns the proposal only if it has an anchor URL (for metadata endpoint).
     * Returns empty if not found or no metadata.
     */
    Optional<BFProposal> findProposalMetadata(String txHash, int index);
}

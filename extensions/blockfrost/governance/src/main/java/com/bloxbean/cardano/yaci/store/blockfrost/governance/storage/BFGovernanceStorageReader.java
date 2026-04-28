package com.bloxbean.cardano.yaci.store.blockfrost.governance.storage;

import com.bloxbean.cardano.yaci.store.blockfrost.governance.storage.impl.model.BFDRepDelegator;
import com.bloxbean.cardano.yaci.store.blockfrost.governance.storage.impl.model.BFDRep;
import com.bloxbean.cardano.yaci.store.blockfrost.governance.storage.impl.model.BFProposal;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.governance.domain.DRepRegistration;
import com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.DRepEntity;

import java.util.List;
import java.util.Optional;

public interface BFGovernanceStorageReader {

    // ── DRep ─────────────────────────────────────────────────────────────────

    /** Paginated list of all unique DReps (drep_id + drep_hash). */
    List<DRepEntity> findAllDReps(int page, int count, Order order);

    /** Latest drep row for the given hex hash, enriched with voting power and script flag. */
    Optional<BFDRep> findDRepByHash(String drepHex);

    /** Delegators for a DRep with their total unspent lovelace. */
    List<BFDRepDelegator> findDRepDelegators(String drepHex, int page, int count, Order order);

    /** All drep_registration cert rows for the given DRep hash. */
    List<DRepRegistration> findDRepUpdates(String drepHex, int page, int count, Order order);

    /** Voting procedure rows for the given DRep voter hash. */
    List<VotingProcedure> findDRepVotes(String drepHex, int page, int count, Order order);

    /** Most recent drep_registration row that has an anchor URL. */
    Optional<DRepRegistration> findDRepMetadata(String drepHex);

    // ── Proposals ────────────────────────────────────────────────────────────

    /** Paginated list of all governance proposals. */
    List<BFProposal> findAllProposals(int page, int count, Order order);

    /** Single proposal identified by tx_hash + index (cert_index). */
    Optional<BFProposal> findProposalByTxHashAndIndex(String txHash, int certIndex);

    /**
     * Returns the proposal only if it is a PARAMETER_CHANGE_ACTION type.
     * Returns empty if not found or wrong type.
     */
    Optional<BFProposal> findParameterChangeProposal(String txHash, int certIndex);

    /**
     * Returns true if the proposal at (txHash, certIndex) is TREASURY_WITHDRAWALS_ACTION.
     */
    boolean isWithdrawalProposal(String txHash, int certIndex);

    /**
     * Treasury withdrawal entries for the given proposal (tx_hash + gov_action_index).
     * Returns address + lovelace amount pairs.
     */
    List<BFDRepDelegator> findProposalWithdrawals(String txHash, int certIndex);

    /** Voting procedure rows for the given proposal. */
    List<VotingProcedure> findProposalVotes(String txHash, int certIndex, int page, int count, Order order);

    /**
     * Returns the proposal only if it has an anchor URL (for metadata endpoint).
     * Returns empty if not found or no metadata.
     */
    Optional<BFProposal> findProposalMetadata(String txHash, int certIndex);
}

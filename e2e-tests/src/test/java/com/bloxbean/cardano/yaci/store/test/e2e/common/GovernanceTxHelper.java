package com.bloxbean.cardano.yaci.store.test.e2e.common;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.backend.api.BackendService;
import com.bloxbean.cardano.client.exception.CborSerializationException;
import com.bloxbean.cardano.client.function.TxSigner;
import com.bloxbean.cardano.client.function.helper.SignerProviders;
import com.bloxbean.cardano.client.governance.GovId;
import com.bloxbean.cardano.client.plutus.spec.PlutusData;
import com.bloxbean.cardano.client.plutus.spec.PlutusV3Script;
import com.bloxbean.cardano.client.quicktx.QuickTxBuilder;
import com.bloxbean.cardano.client.quicktx.Tx;
import com.bloxbean.cardano.client.spec.UnitInterval;
import com.bloxbean.cardano.client.transaction.spec.ProtocolParamUpdate;
import com.bloxbean.cardano.client.transaction.spec.ProtocolVersion;
import com.bloxbean.cardano.client.transaction.spec.Withdrawal;
import com.bloxbean.cardano.client.transaction.spec.governance.Anchor;
import com.bloxbean.cardano.client.transaction.spec.governance.Constitution;
import com.bloxbean.cardano.client.transaction.spec.governance.DRep;
import com.bloxbean.cardano.client.transaction.spec.governance.Vote;
import com.bloxbean.cardano.client.transaction.spec.governance.Voter;
import com.bloxbean.cardano.client.transaction.spec.governance.VoterType;
import com.bloxbean.cardano.client.transaction.spec.governance.actions.GovAction;
import com.bloxbean.cardano.client.transaction.spec.governance.actions.HardForkInitiationAction;
import com.bloxbean.cardano.client.transaction.spec.governance.actions.InfoAction;
import com.bloxbean.cardano.client.transaction.spec.governance.actions.NewConstitution;
import com.bloxbean.cardano.client.transaction.spec.governance.actions.NoConfidence;
import com.bloxbean.cardano.client.transaction.spec.governance.actions.ParameterChangeAction;
import com.bloxbean.cardano.client.transaction.spec.governance.actions.TreasuryWithdrawalsAction;
import com.bloxbean.cardano.client.transaction.spec.governance.actions.UpdateCommittee;
import com.bloxbean.cardano.client.transaction.spec.cert.AuthCommitteeHotCert;
import com.bloxbean.cardano.client.transaction.spec.cert.Certificate;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.store.governance.domain.GovActionProposal;
import com.bloxbean.cardano.yaci.store.governance.storage.GovActionProposalStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.bloxbean.cardano.client.common.ADAConversionUtil.adaToLovelace;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class GovernanceTxHelper extends TransactionHelper {
    private static final Logger log = LoggerFactory.getLogger(GovernanceTxHelper.class);

    private static final int DEFAULT_ACTION_INDEX = 0;
    private static final String DEFAULT_ANCHOR_HASH = "cafef700c0039a2efb056a665b3a8bcd94f8670b88d659f7f3db68340f6f0937";
    private static final String DEFAULT_CONSTITUTION_HASH = "daeef700c0039a2efb056a665b3a8bcd94f8670b88d659f7f3db68340f6f0937";

    public static final PlutusV3Script ALWAYS_TRUE_SCRIPT = PlutusV3Script.builder()
            .type("PlutusScriptV3")
            .cborHex("46450101002499")
            .build();

    private final BackendService backendService;
    private final GovActionProposalStorage govActionProposalStorage;
    private final int govActionLifetime;

    public GovernanceTxHelper(BackendService backendService,
                              GovActionProposalStorage govActionProposalStorage,
                              int govActionLifetime) {
        super(backendService);
        this.backendService = backendService;
        this.govActionProposalStorage = govActionProposalStorage;
        this.govActionLifetime = govActionLifetime;
    }

    public record CreatedProposal(String txHash,
                                  int index,
                                  int createdEpoch,
                                  int maxVotingEpoch,
                                  int expiryStatusEpoch,
                                  GovActionProposal proposal) {
        public GovActionId storeGovActionId() {
            return new GovActionId(txHash, index);
        }

        public com.bloxbean.cardano.client.transaction.spec.governance.actions.GovActionId txGovActionId() {
            return new com.bloxbean.cardano.client.transaction.spec.governance.actions.GovActionId(txHash, index);
        }
    }

    public static Anchor anchor(String url, String anchorHashHex) {
        return new Anchor(url, HexUtil.decodeHexString(anchorHashHex));
    }

    public static Anchor defaultAnchor() {
        return anchor("https://xyz.com", DEFAULT_ANCHOR_HASH);
    }

    public static Anchor defaultConstitutionAnchor() {
        return anchor("https://constitution.example.com", DEFAULT_CONSTITUTION_HASH);
    }

    public static InfoAction infoAction() {
        return new InfoAction();
    }

    public static ParameterChangeAction parameterChangeAction() {
        return parameterChangeAction(ProtocolParamUpdate.builder()
                .minPoolCost(adaToLovelace(100))
                .build(), alwaysTrueScriptHash());
    }

    public static ParameterChangeAction parameterChangeAction(ProtocolParamUpdate protocolParamUpdate, byte[] policyHash) {
        var parameterChangeAction = new ParameterChangeAction();
        parameterChangeAction.setProtocolParamUpdate(protocolParamUpdate);
        parameterChangeAction.setPolicyHash(policyHash);
        return parameterChangeAction;
    }

    public static TreasuryWithdrawalsAction treasuryWithdrawalsAction(String rewardAddress, BigInteger lovelace) {
        var treasuryWithdrawalsAction = new TreasuryWithdrawalsAction();
        treasuryWithdrawalsAction.addWithdrawal(new Withdrawal(rewardAddress, lovelace));
        treasuryWithdrawalsAction.setPolicyHash(alwaysTrueScriptHash());
        return treasuryWithdrawalsAction;
    }

    public static HardForkInitiationAction hardForkInitiationAction(int major, int minor) {
        var hardForkInitiationAction = new HardForkInitiationAction();
        hardForkInitiationAction.setProtocolVersion(new ProtocolVersion(major, minor));
        return hardForkInitiationAction;
    }

    public static NoConfidence noConfidenceAction() {
        return new NoConfidence();
    }

    public static NoConfidence noConfidenceAction(com.bloxbean.cardano.client.transaction.spec.governance.actions.GovActionId prevGovActionId) {
        var noConfidence = new NoConfidence();
        noConfidence.setPrevGovActionId(prevGovActionId);
        return noConfidence;
    }

    public static UpdateCommittee updateCommitteeAction() {
        return updateCommitteeAction(new UnitInterval(BigInteger.ONE, BigInteger.valueOf(3)));
    }

    public static UpdateCommittee updateCommitteeAction(UnitInterval quorumThreshold) {
        var updateCommittee = new UpdateCommittee();
        updateCommittee.setQuorumThreshold(quorumThreshold);
        return updateCommittee;
    }

    public static NewConstitution newConstitutionAction() {
        return newConstitutionAction(defaultConstitutionAnchor());
    }

    public static NewConstitution newConstitutionAction(Anchor constitutionAnchor) {
        var newConstitution = new NewConstitution();
        newConstitution.setConstitution(Constitution.builder()
                .anchor(constitutionAnchor)
                .build());
        return newConstitution;
    }

    public CreatedProposal createInfoProposalAndWait(Account proposalCreator, String returnStakeAddress) {
        return createProposalAndWait(proposalCreator, returnStakeAddress, infoAction(), defaultAnchor());
    }

    public CreatedProposal createProposalAndWait(Account proposalCreator,
                                                 String returnStakeAddress,
                                                 GovAction govAction) {
        return createProposalAndWait(proposalCreator, returnStakeAddress, govAction, defaultAnchor());
    }

    public CreatedProposal createProposalAndWait(Account proposalCreator,
                                                 String returnStakeAddress,
                                                 GovAction govAction,
                                                 Anchor anchor) {
        var result = submitProposal(proposalCreator, returnStakeAddress, govAction, anchor);
        return waitForCreatedProposal(result.getValue(), DEFAULT_ACTION_INDEX);
    }

    public Result<String> submitProposal(Account proposalCreator,
                                         String returnStakeAddress,
                                         GovAction govAction,
                                         Anchor anchor) {
        var quickTxBuilder = new QuickTxBuilder(backendService);
        Tx tx;

        if (requiresProposingScript(govAction)) {
            tx = new Tx()
                    .createProposal(govAction, returnStakeAddress, anchor, PlutusData.unit())
                    .attachProposingValidator(ALWAYS_TRUE_SCRIPT)
                    .from(proposalCreator.baseAddress());
        } else {
            tx = new Tx()
                    .createProposal(govAction, returnStakeAddress, anchor)
                    .from(proposalCreator.baseAddress());
        }

        log.info("Creating governance proposal. returnAddress={}, action={}", returnStakeAddress, govAction.getClass().getSimpleName());
        var result = quickTxBuilder.compose(tx)
                .withSigner(SignerProviders.drepKeySignerFrom(proposalCreator))
                .withSigner(SignerProviders.signerFrom(proposalCreator))
                .completeAndWait(System.out::println);

        assertSuccessful(result);
        checkIfUtxoAvailable(result.getValue(), proposalCreator.baseAddress());
        return result;
    }

    public CreatedProposal waitForCreatedProposal(String txHash, int index) {
        var govActionId = new GovActionId(txHash, index);
        var proposalRef = new AtomicReference<GovActionProposal>();

        await().atMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofSeconds(1))
                .until(() -> {
                    var proposal = govActionProposalStorage.findByGovActionIds(List.of(govActionId))
                            .stream()
                            .findFirst()
                            .orElse(null);
                    proposalRef.set(proposal);
                    return proposal != null;
                });

        return toCreatedProposal(proposalRef.get());
    }

    public CreatedProposal toCreatedProposal(GovActionProposal proposal) {
        int createdEpoch = proposal.getEpoch();
        return new CreatedProposal(
                proposal.getTxHash(),
                (int) proposal.getIndex(),
                createdEpoch,
                createdEpoch + govActionLifetime,
                createdEpoch + govActionLifetime + 1,
                proposal);
    }

    public Result<String> registerDRep(Account drepAccount) {
        return registerDRep(drepAccount, drepAccount, defaultAnchor());
    }

    public Result<String> registerDRep(Account feePayer, Account drepAccount, Anchor anchor) {
        var tx = new Tx()
                .registerDRep(drepAccount, anchor)
                .from(feePayer.baseAddress());

        var result = new QuickTxBuilder(backendService).compose(tx)
                .withSigner(SignerProviders.signerFrom(feePayer))
                .withSigner(SignerProviders.drepKeySignerFrom(drepAccount))
                .completeAndWait(System.out::println);

        assertSuccessful(result);
        checkIfUtxoAvailable(result.getValue(), feePayer.baseAddress());
        return result;
    }

    public Result<String> delegateVotingPowerToDRep(Account delegator, Account drepAccount) {
        return delegateVotingPowerToDRep(delegator, delegator, GovId.toDrep(drepAccount.drepId()));
    }

    public Result<String> delegateVotingPowerToDRep(Account feePayer, Account delegator, DRep drep) {
        var tx = new Tx()
                .delegateVotingPowerTo(delegator.baseAddress(), drep)
                .from(feePayer.baseAddress());

        var result = new QuickTxBuilder(backendService).compose(tx)
                .withSigner(SignerProviders.signerFrom(feePayer))
                .withSigner(SignerProviders.stakeKeySignerFrom(delegator))
                .completeAndWait(System.out::println);

        assertSuccessful(result);
        checkIfUtxoAvailable(result.getValue(), feePayer.baseAddress());
        return result;
    }

    public Result<String> delegateVotingPowerToAlwaysAbstain(Account feePayer, Account delegator) {
        return delegateVotingPowerToDRep(feePayer, delegator, DRep.abstain());
    }

    public Result<String> delegateVotingPowerToAlwaysNoConfidence(Account feePayer, Account delegator) {
        return delegateVotingPowerToDRep(feePayer, delegator, DRep.noConfidence());
    }

    public Result<String> castDRepVote(Account feePayer,
                                       Account drepAccount,
                                       GovActionId govActionId,
                                       Vote vote) {
        return castDRepVote(feePayer, drepAccount, govActionId, vote, defaultAnchor());
    }

    public Result<String> castDRepVote(Account feePayer,
                                       Account drepAccount,
                                       GovActionId govActionId,
                                       Vote vote,
                                       Anchor anchor) {
        var voter = new Voter(VoterType.DREP_KEY_HASH, drepAccount.drepCredential());
        return castVote(feePayer, voter, clientGovActionId(govActionId), vote, anchor,
                SignerProviders.drepKeySignerFrom(drepAccount));
    }

    public Result<String> castCommitteeHotVote(Account feePayer,
                                               Account committeeHotAccount,
                                               GovActionId govActionId,
                                               Vote vote) {
        var voter = new Voter(VoterType.CONSTITUTIONAL_COMMITTEE_HOT_KEY_HASH, committeeHotAccount.committeeHotCredential());
        return castVote(feePayer, voter, clientGovActionId(govActionId), vote, defaultAnchor(),
                SignerProviders.committeeHotKeySignerFrom(committeeHotAccount));
    }

    public Result<String> castStakePoolVote(Account feePayer,
                                            Voter stakePoolVoter,
                                            GovActionId govActionId,
                                            Vote vote,
                                            TxSigner stakePoolSigner) {
        return castVote(feePayer, stakePoolVoter, clientGovActionId(govActionId), vote, defaultAnchor(), stakePoolSigner);
    }

    public Result<String> castVote(Account feePayer,
                                   Voter voter,
                                   com.bloxbean.cardano.client.transaction.spec.governance.actions.GovActionId govActionId,
                                   Vote vote,
                                   Anchor anchor,
                                   TxSigner... additionalSigners) {
        var tx = new Tx()
                .createVote(voter, govActionId, vote, anchor)
                .from(feePayer.baseAddress());

        var txContext = new QuickTxBuilder(backendService).compose(tx)
                .withSigner(SignerProviders.signerFrom(feePayer));

        if (additionalSigners != null) {
            for (TxSigner signer : additionalSigners) {
                txContext.withSigner(signer);
            }
        }

        var result = txContext.completeAndWait(System.out::println);
        assertSuccessful(result);
        checkIfUtxoAvailable(result.getValue(), feePayer.baseAddress());
        return result;
    }

    public Result<String> authorizeCommitteeHotKey(Account feePayer,
                                                   Account committeeColdAccount,
                                                   Account committeeHotAccount) {
        var cert = new AuthCommitteeHotCert(
                committeeColdAccount.committeeColdCredential(),
                committeeHotAccount.committeeHotCredential());

        var tx = new Tx()
                .from(feePayer.baseAddress());

        var result = new QuickTxBuilder(backendService).compose(tx)
                .preBalanceTx((context, transaction) -> {
                    if (transaction.getBody().getCerts() == null) {
                        transaction.getBody().setCerts(new ArrayList<Certificate>());
                    }
                    transaction.getBody().getCerts().add(cert);
                })
                .withSigner(SignerProviders.signerFrom(feePayer))
                .withSigner(SignerProviders.committeeColdKeySignerFrom(committeeColdAccount))
                .completeAndWait(System.out::println);

        assertSuccessful(result);
        checkIfUtxoAvailable(result.getValue(), feePayer.baseAddress());
        return result;
    }

    public static com.bloxbean.cardano.client.transaction.spec.governance.actions.GovActionId clientGovActionId(GovActionId govActionId) {
        return new com.bloxbean.cardano.client.transaction.spec.governance.actions.GovActionId(
                govActionId.getTransactionId(), govActionId.getGov_action_index());
    }

    private boolean requiresProposingScript(GovAction govAction) {
        if (govAction instanceof ParameterChangeAction parameterChangeAction) {
            return parameterChangeAction.getPolicyHash() != null && parameterChangeAction.getPolicyHash().length > 0;
        }

        if (govAction instanceof TreasuryWithdrawalsAction treasuryWithdrawalsAction) {
            return treasuryWithdrawalsAction.getPolicyHash() != null && treasuryWithdrawalsAction.getPolicyHash().length > 0;
        }

        return false;
    }

    private static byte[] alwaysTrueScriptHash() {
        try {
            return ALWAYS_TRUE_SCRIPT.getScriptHash();
        } catch (CborSerializationException e) {
            throw new IllegalStateException("Unable to compute default always-true script hash", e);
        }
    }

    private void assertSuccessful(Result<String> result) {
        log.info("Governance transaction result: {}", result);
        assertThat(result.isSuccessful())
                .as(result.toString())
                .isTrue();
    }
}

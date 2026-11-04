package com.bloxbean.cardano.yaci.store.script.helper;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.address.CredentialType;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.core.model.*;
import com.bloxbean.cardano.yaci.core.model.certs.*;
import com.bloxbean.cardano.yaci.core.model.governance.Voter;
import com.bloxbean.cardano.yaci.helper.model.Transaction;
import com.bloxbean.cardano.yaci.store.client.utxo.UtxoClient;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A utility component to match a redeemer to available scripts based on mint tag
 */
@Component
@Slf4j
@AllArgsConstructor
public class RedeemerDatumMatcher {

    private UtxoClient utxoClient;

    /**
     * Find scripts for redeemers in a transaction
     * @param transaction
     * @param scriptsMap
     * @return List of Tuple of {@link com.bloxbean.cardano.yaci.core.model.Redeemer} and {@link PlutusScript}
     */
    public List<ScriptContext> findScriptsForRedeemers(Transaction transaction, Map<String, PlutusScript> scriptsMap) {
        //Map redeemers to scripts
        //Find distinct policies if mint available. Mint redeemer index follows the ledger's policy id order, not the CBOR order.
        List<String> distinctPolicies = transaction.getBody().getMint() != null ? transaction.getBody().getMint().stream()
                .map(amount -> amount.getPolicyId())
                .distinct()
                .sorted()
                .collect(Collectors.toList()) : null;

        List<TransactionInput> inputs = new ArrayList<>(transaction.getBody().getInputs());
        List<TransactionInput> sortedInputs = getSortedInputs(inputs);
        List<com.bloxbean.cardano.yaci.core.model.Redeemer> redeemers = transaction.getWitnesses().getRedeemers();
        List<ScriptContext> scriptContexts = redeemers.stream()
                .map(redeemer -> {
                    ScriptContext scriptContext;
                    if (redeemer.getTag() == RedeemerTag.Spend) {
                        scriptContext = findSpendScriptFromRedeemer(redeemer, sortedInputs, scriptsMap)
                                .orElse(new ScriptContext());
                        scriptContext.setRedeemer(redeemer); //set redeemer here to save serialization cost
                    } else if (redeemer.getTag() == RedeemerTag.Mint) {
                        if (log.isDebugEnabled())
                            log.debug("Mint tag : " + transaction.getTxHash());
                        //check mint policy
                        scriptContext = findMintScriptForRedeemer(redeemer, distinctPolicies, scriptsMap)
                                .orElse(new ScriptContext());
                        scriptContext.setRedeemer(redeemer);
                    } else if (redeemer.getTag() == RedeemerTag.Cert) {
                        if (log.isDebugEnabled())
                            log.debug("Redeemer Cert : " + transaction.getTxHash());
                        scriptContext = findCertScriptForRedeemer(redeemer, transaction.getBody().getCertificates(), scriptsMap)
                                .orElse(new ScriptContext());
                        scriptContext.setRedeemer(redeemer);
                    } else if (redeemer.getTag() == RedeemerTag.Reward) {
                        if (log.isDebugEnabled())
                            log.info("Redeemer Reward : " + transaction.getTxHash());
                        scriptContext = findRewardScriptForRedeemer(redeemer, transaction, scriptsMap).orElse(new ScriptContext());
                        scriptContext.setRedeemer(redeemer);
                    } else if (redeemer.getTag() == RedeemerTag.Voting) {
                        if (log.isDebugEnabled())
                            log.info("Redeemer Voting : " + transaction.getTxHash());
                        scriptContext = findVotingScriptForRedeemer(redeemer, transaction, scriptsMap).orElse(new ScriptContext());
                        scriptContext.setRedeemer(redeemer);
                    } else if (redeemer.getTag() == RedeemerTag.Proposing) {
                        if (log.isDebugEnabled())
                            log.info("Redeemer Proposing : " + transaction.getTxHash());
                        scriptContext = findProposingScriptForRedeemer(redeemer, transaction, scriptsMap).orElse(new ScriptContext());
                        scriptContext.setRedeemer(redeemer);
                    } else {
                        scriptContext = new ScriptContext();
                        scriptContext.setRedeemer(redeemer);
                    }

                    return scriptContext;

                }).collect(Collectors.toList());
        return scriptContexts;
    }

    public Optional<ScriptContext> findSpendScriptFromRedeemer(Redeemer redeemer, List<TransactionInput> sortedInputs, Map<String, PlutusScript> scriptsMap) {
        int index = redeemer.getIndex();
        TransactionInput input = sortedInputs.get(index);

        //Find out if any script addressed inputs
        Optional<ScriptContext> scriptContext = utxoClient.getUtxoById(new UtxoKey(input.getTransactionId(), input.getIndex()))
                .map(addressUtxo -> {
                    String paymentKeyHash = addressUtxo.getOwnerPaymentCredential();
                    String stakeKeyHash = addressUtxo.getOwnerStakeCredential();

                    PlutusScript script = scriptsMap.get(paymentKeyHash);
                    if (script == null) { //Check if stake key hash is matching //TODO -- need to check if required
                        script = scriptsMap.get(stakeKeyHash);
                    }

                    //Check if inline datum availabe, set it to scriptcontext
                    String inlineDatum = addressUtxo.getInlineDatum();
                    String datumHash = addressUtxo.getDataHash();

                    ScriptContext scContext = new ScriptContext();
                    scContext.setPlutusScript(script);
                    scContext.setDatum(inlineDatum);
                    scContext.setDatumHash(datumHash);

                    return Optional.of(scContext);
                }).orElse(Optional.empty());
        return scriptContext;
    }

    public Optional<ScriptContext> findRewardScriptForRedeemer(Redeemer redeemer, Transaction transaction, Map<String, PlutusScript> scriptMap) {
        Map<String, BigInteger> withdrawals = transaction.getBody().getWithdrawals();
        if (withdrawals == null || withdrawals.size() == 0)
            return Optional.empty();

        //Reward redeemer index follows the ledger's reward account order: script credentials first, then credential hash
        List<String> rewardAddresses = withdrawals.keySet().stream()
                .sorted(Comparator.comparing(RedeemerDatumMatcher::isKeyCredential)
                        .thenComparing(RedeemerDatumMatcher::delegationHash))
                .collect(Collectors.toList());

        int index = redeemer.getIndex();
        if (index >= rewardAddresses.size())
            return Optional.empty();

        String delegationHash = delegationHash(rewardAddresses.get(index));

        PlutusScript plutusScript = null;
        if (delegationHash != null)
            plutusScript = scriptMap.get(delegationHash);

        ScriptContext scriptContext = new ScriptContext();
        scriptContext.setPlutusScript(plutusScript);

        return Optional.of(scriptContext);
    }

    public Optional<ScriptContext> findCertScriptForRedeemer(Redeemer redeemer, List<Certificate> certificates, Map<String, PlutusScript> scriptMap) {
        if (certificates == null ||certificates.size() == 0)
            return Optional.empty();

        int index = redeemer.getIndex();

        if (index >= certificates.size())
            return Optional.empty();

        Certificate certificate = certificates.get(index);
        String credentialHash;
        if (certificate instanceof StakeRegistration cert)
            credentialHash = credentialHash(cert.getStakeCredential());
        else if (certificate instanceof StakeDelegation cert)
            credentialHash = credentialHash(cert.getStakeCredential());
        else if (certificate instanceof StakeDeregistration cert)
            credentialHash = credentialHash(cert.getStakeCredential());
        else if (certificate instanceof RegCert cert)
            credentialHash = credentialHash(cert.getStakeCredential());
        else if (certificate instanceof UnregCert cert)
            credentialHash = credentialHash(cert.getStakeCredential());
        else if (certificate instanceof VoteDelegCert cert)
            credentialHash = credentialHash(cert.getStakeCredential());
        else if (certificate instanceof StakeVoteDelegCert cert)
            credentialHash = credentialHash(cert.getStakeCredential());
        else if (certificate instanceof StakeRegDelegCert cert)
            credentialHash = credentialHash(cert.getStakeCredential());
        else if (certificate instanceof VoteRegDelegCert cert)
            credentialHash = credentialHash(cert.getStakeCredential());
        else if (certificate instanceof StakeVoteRegDelegCert cert)
            credentialHash = credentialHash(cert.getStakeCredential());
        else if (certificate instanceof AuthCommitteeHotCert cert)
            credentialHash = credentialHash(cert.getCommitteeColdCredential());
        else if (certificate instanceof ResignCommitteeColdCert cert)
            credentialHash = credentialHash(cert.getCommitteeColdCredential());
        else if (certificate instanceof RegDrepCert cert)
            credentialHash = credentialHash(cert.getDrepCredential());
        else if (certificate instanceof UnregDrepCert cert)
            credentialHash = credentialHash(cert.getDrepCredential());
        else if (certificate instanceof UpdateDrepCert cert)
            credentialHash = credentialHash(cert.getDrepCredential());
        else
            credentialHash = null;

        //TODO -- For other certificate types ??
        //PoolRegistration, PoolRetirement, GenesisKeyDelegation, MoveInstantaneousRewardsCert

        PlutusScript plutusScript = null;
        if (credentialHash != null)
            plutusScript = scriptMap.get(credentialHash);

        ScriptContext scriptContext = new ScriptContext();
        scriptContext.setPlutusScript(plutusScript);

        return Optional.of(scriptContext);
    }

    public Optional<ScriptContext> findMintScriptForRedeemer(Redeemer redeemer, List<String> policies, Map<String, PlutusScript> scriptMap) {
        if (policies == null)
            return Optional.empty();

        int index = redeemer.getIndex();
        if (index >= policies.size()) {
            return Optional.empty();
        }

        String policyId = policies.get(index);
        PlutusScript plutusScript = scriptMap.get(policyId);

        ScriptContext scriptContext = new ScriptContext();
        scriptContext.setPlutusScript(plutusScript);

        return Optional.of(scriptContext);
    }

    public Optional<ScriptContext> findVotingScriptForRedeemer(Redeemer redeemer, Transaction transaction, Map<String, PlutusScript> scriptMap) {
        var votingProcedures = transaction.getBody().getVotingProcedures();
        if (votingProcedures == null || votingProcedures.getVoting() == null || votingProcedures.getVoting().size() == 0)
            return Optional.empty();

        //Voting redeemer index follows the ledger's voter order: committee, DRep, pool; script credentials before key credentials
        List<Voter> voters = votingProcedures.getVoting().keySet().stream()
                .sorted(Comparator.comparing(RedeemerDatumMatcher::voterRank)
                        .thenComparing(Voter::getHash))
                .collect(Collectors.toList());

        if (redeemer.getIndex() >= voters.size())
            return Optional.empty();

        var hash = voters.get(redeemer.getIndex()).getHash();

        PlutusScript plutusScript = null;
        if (hash != null)
            plutusScript = scriptMap.get(hash);

        ScriptContext scriptContext = new ScriptContext();
        scriptContext.setPlutusScript(plutusScript);

        return Optional.of(scriptContext);
    }

    public Optional<ScriptContext> findProposingScriptForRedeemer(Redeemer redeemer, Transaction transaction, Map<String, PlutusScript> scriptMap) {
        var proposalProcedures = transaction.getBody().getProposalProcedures();
        if (proposalProcedures == null || proposalProcedures.size() == 0)
            return Optional.empty();

        if (redeemer.getIndex() >= proposalProcedures.size())
            return Optional.empty();

        //TODO - Get the constitution policy (script hash)
        //For now set scriptHash to null

        ScriptContext scriptContext = new ScriptContext();
        scriptContext.setPlutusScript(null);

        return Optional.of(scriptContext);
    }

    private static List<TransactionInput> getSortedInputs(List<TransactionInput> inputs) {
        List<TransactionInput> copyInputs = inputs
                .stream()
                .collect(Collectors.toList());
        copyInputs.sort(
                Comparator.comparing(TransactionInput::getTransactionId)
                        .thenComparing(TransactionInput::getIndex)
        );
        return copyInputs;
    }

    private static boolean isKeyCredential(String rewardAddress) {
        return new Address(HexUtil.decodeHexString(rewardAddress)).getDelegationCredential().get().getType() == CredentialType.Key;
    }

    private static String delegationHash(String rewardAddress) {
        Address address = new Address(HexUtil.decodeHexString(rewardAddress));
        return HexUtil.encodeHexString(address.getDelegationCredential().get().getBytes());
    }

    private static int voterRank(Voter voter) {
        return switch (voter.getType()) {
            case CONSTITUTIONAL_COMMITTEE_HOT_SCRIPT_HASH -> 0;
            case CONSTITUTIONAL_COMMITTEE_HOT_KEY_HASH -> 1;
            case DREP_SCRIPT_HASH -> 2;
            case DREP_KEY_HASH -> 3;
            case STAKING_POOL_KEY_HASH -> 4;
        };
    }

    private String credentialHash(StakeCredential stakeCredential) {
        return stakeCredential != null? stakeCredential.getHash() : null;
    }

    private String credentialHash(Credential credential) {
        return credential != null? credential.getHash() : null;
    }
}

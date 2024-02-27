package com.bloxbean.cardano.yaci.store.script.helper;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.core.model.PlutusScript;
import com.bloxbean.cardano.yaci.core.model.Redeemer;
import com.bloxbean.cardano.yaci.core.model.RedeemerTag;
import com.bloxbean.cardano.yaci.core.model.TransactionInput;
import com.bloxbean.cardano.yaci.core.model.certs.*;
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
        //Find distinct policies if mint available
        List<String> distinctPolicies = transaction.getBody().getMint() != null ? transaction.getBody().getMint().stream()
                .map(amount -> amount.getPolicyId())
                .distinct()
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
                    } else {
                        scriptContext = new ScriptContext();
                        scriptContext.setRedeemer(redeemer);
                    }

                    //TODO -- Handle governance related purpose

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

        List<String> rewardAddresses = withdrawals.entrySet().stream()
                .map(entry -> entry.getKey())
                .collect(Collectors.toList());

        int index = redeemer.getIndex();
        String rewardAddress = rewardAddresses.get(index);

        Address address = new Address(HexUtil.decodeHexString(rewardAddress));
        String delegationHash = HexUtil.encodeHexString(address.getDelegationCredential().get().getBytes());

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
        StakeCredential stakeCredential = null;
        if (certificate instanceof StakeRegistration)
            stakeCredential = ((StakeRegistration) certificate).getStakeCredential();
        else if (certificate instanceof StakeDelegation)
            stakeCredential = ((StakeDelegation) certificate).getStakeCredential();
        else if (certificate instanceof StakeDeregistration)
            stakeCredential = ((StakeDeregistration) certificate).getStakeCredential();

        //TODO -- For other certificate types ??
        //PoolRegistration, PoolRetirement, GenesisKeyDelegation, MoveInstantaneousRewardsCert

        PlutusScript plutusScript = null;
        if (stakeCredential != null)
            plutusScript = scriptMap.get(stakeCredential.getHash());

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
}

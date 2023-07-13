package com.bloxbean.cardano.yaci.store.script.helper;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.plutus.spec.Redeemer;
import com.bloxbean.cardano.client.plutus.spec.RedeemerTag;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import com.bloxbean.cardano.yaci.store.common.util.Util;
import com.bloxbean.cardano.client.util.Tuple;
import com.bloxbean.cardano.yaci.core.model.PlutusScript;
import com.bloxbean.cardano.yaci.core.model.TransactionInput;
import com.bloxbean.cardano.yaci.core.model.certs.*;
import com.bloxbean.cardano.yaci.helper.model.Transaction;
import com.bloxbean.cardano.yaci.store.client.utxo.UtxoClient;
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
        List<com.bloxbean.cardano.yaci.core.model.Redeemer> redeemers = transaction.getWitnesses().getRedeemers();
        List<ScriptContext> scriptContexts = redeemers.stream()
                .map(redeemer -> Util.deserialize(redeemer.getCbor())
                        .map(deRedeemer -> new Tuple<>(redeemer, deRedeemer)))
                .filter(Optional::isPresent)
                .map(redeemerTuple -> {
                    com.bloxbean.cardano.yaci.core.model.Redeemer orgRedeemer = redeemerTuple.get()._1;
                    com.bloxbean.cardano.client.plutus.spec.Redeemer deRedeemer = redeemerTuple.get()._2;

                    if (deRedeemer.getTag() == RedeemerTag.Spend) {
                        ScriptContext scriptContext = findSpendScriptFromRedeemer(deRedeemer, inputs, scriptsMap)
                                .orElse(new ScriptContext());
                        scriptContext.setRedeemer(orgRedeemer.getCbor()); //set redeemer here to save serialization cost
                        return scriptContext;
                    }
                    if (deRedeemer.getTag() == RedeemerTag.Mint) {
                        if (log.isDebugEnabled())
                            log.debug("Mint tag : " + transaction.getTxHash());
                        //check mint policy
                        ScriptContext scriptContext = findMintScriptForRedeemer(deRedeemer, distinctPolicies, scriptsMap)
                                .orElse(new ScriptContext());
                        scriptContext.setRedeemer(orgRedeemer.getCbor());
                        return scriptContext;
                    } else if (deRedeemer.getTag() == RedeemerTag.Cert) {
                        if (log.isDebugEnabled())
                            log.debug("Redeemer Cert : " + transaction.getTxHash());
                        ScriptContext scriptContext = findCertScriptForRedeemer(deRedeemer, transaction.getBody().getCertificates(), scriptsMap)
                                .orElse(new ScriptContext());
                        scriptContext.setRedeemer(orgRedeemer.getCbor());
                        return scriptContext;
                    } else if (deRedeemer.getTag() == RedeemerTag.Reward) {
                        if (log.isDebugEnabled())
                            log.info("Redeemer Reward : " + transaction.getTxHash());
                        ScriptContext scriptContext = findRewardScriptForRedeemer(deRedeemer, transaction, scriptsMap).orElse(new ScriptContext());
                        scriptContext.setRedeemer(orgRedeemer.getCbor());
                        return scriptContext;
                    } else {
                        ScriptContext scriptContext = new ScriptContext();
                        scriptContext.setRedeemer(orgRedeemer.getCbor());
                        return scriptContext;
                    }
                }).collect(Collectors.toList());
        return scriptContexts;
    }

    public Optional<ScriptContext> findSpendScriptFromRedeemer(Redeemer redeemer,
                                                              List<TransactionInput> inputs, Map<String, PlutusScript> scriptsMap) {
        //Sort inputs and find the right input for redeemer
        TransactionInput input = getScriptInputFromRedeemer(redeemer, inputs);

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

        int index = redeemer.getIndex().intValue();
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

        int index = redeemer.getIndex().intValue();

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

        int index = redeemer.getIndex().intValue();
        if (index >= policies.size()) {
            return Optional.empty();
        }

        String policyId = policies.get(index);
        PlutusScript plutusScript = scriptMap.get(policyId);

        ScriptContext scriptContext = new ScriptContext();
        scriptContext.setPlutusScript(plutusScript);

        return Optional.of(scriptContext);
    }

    //Sort inputs and then find the right input for the redeemer's index field
    public static TransactionInput getScriptInputFromRedeemer(Redeemer redeemer, List<TransactionInput> inputs) {
        //Sorting required to find the correct input. This is the current behavior in the node.
        List<TransactionInput> copyInputs = inputs
                .stream()
                .collect(Collectors.toList());
        copyInputs.sort((o1, o2) -> (o1.getTransactionId() + "#" + o1.getIndex()).compareTo(o2.getTransactionId() + "#" + o2.getIndex()));

        int index = redeemer.getIndex().intValue();
        return copyInputs.get(index);
    }
}

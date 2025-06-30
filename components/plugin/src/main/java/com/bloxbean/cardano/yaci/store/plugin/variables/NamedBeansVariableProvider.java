package com.bloxbean.cardano.yaci.store.plugin.variables;

import com.bloxbean.cardano.yaci.store.plugin.api.VariableProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
* This class provides a way to access various storage readers by their bean names from plugins.
**/
@Component
@RequiredArgsConstructor
public class NamedBeansVariableProvider implements VariableProvider {

    private final ApplicationContext applicationContext;

    @Override
    public Map<String, Object> getVariables() {
        Map<String, Object> variables = new HashMap<>();

        try {
            variables.put("asset_reader", applicationContext.getBean("assetStorageReader"));
        } catch (Exception ignored) {}

        try {
            variables.put("block_reader", applicationContext.getBean("blockStorageReader"));
        } catch (Exception ignored) {}

        try {
            variables.put("epoch_param_reader", applicationContext.getBean("epochParamStorage"));
        } catch (Exception ignored) {}

        //governance
        try {
            variables.put("voting_procedure_reader", applicationContext.getBean("votingProcedureStorageReader"));
        } catch (Exception ignored) {}

        try {
            variables.put("delegation_vote_reader", applicationContext.getBean("delegationVoteStorageReader"));
        } catch (Exception ignored) {}

        try {
            variables.put("committee_reader", applicationContext.getBean("committeeStorageReader"));
        } catch (Exception ignored) {}

        try {
            variables.put("gov_action_reader", applicationContext.getBean("govActionProposalStorageReader"));
        } catch (Exception ignored) {}

        try {
            variables.put("constitution_reader", applicationContext.getBean("constitutionStorageReader"));
        } catch (Exception ignored) {}

        try {
            variables.put("metadata_reader", applicationContext.getBean("txMetadataStorageReader"));
        } catch (Exception ignored) {}

        try {
            variables.put("mir_reader", applicationContext.getBean("mirStorageReader"));
        } catch (Exception ignored) {}

        try {
            variables.put("datum_reader", applicationContext.getBean("datumStorageReader"));
        } catch (Exception ignored) {}

        try {
            variables.put("script_reader", applicationContext.getBean("scriptStorageReader"));
        } catch (Exception ignored) {}

        try {
            variables.put("tx_script_reader", applicationContext.getBean("txScriptStorageReader"));
        } catch (Exception ignored) {}

        try {
            variables.put("pool_cert_reader", applicationContext.getBean("poolCertificateStorageReader"));
        } catch (Exception ignored) {}

        try {
            variables.put("pool_reader", applicationContext.getBean("poolStorageReader"));
        } catch (Exception ignored) {}

        try {
            variables.put("staking_cert_reader", applicationContext.getBean("stakingCertificateStorageReader"));
        } catch (Exception ignored) {}

        try {
            variables.put("txn_reader", applicationContext.getBean("transactionStorageReader"));
        } catch (Exception ignored) {}

        try {
            variables.put("txn_witness_reader", applicationContext.getBean("transactionWitnessStorageReader"));
        } catch (NoSuchBeanDefinitionException ignored) {}

        try {
            variables.put("withdrawal_reader", applicationContext.getBean("withdrawalStorageReader"));
        } catch (Exception ignored) {}

        try {
            variables.put("utxo_reader", applicationContext.getBean("utxoStorageReader"));
        } catch (Exception ignored) {}

        try {
            variables.put("drep_reader", applicationContext.getBean("dRepStorageReader"));
        } catch (Exception ignored) {}

        try {
            variables.put("proposal_status_reader", applicationContext.getBean("govActionProposalStatusStorageReader"));
        } catch (Exception ignored) {}

        return variables;
    }
}


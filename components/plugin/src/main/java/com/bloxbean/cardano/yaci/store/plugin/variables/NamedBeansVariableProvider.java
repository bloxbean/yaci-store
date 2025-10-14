package com.bloxbean.cardano.yaci.store.plugin.variables;

import com.bloxbean.cardano.yaci.store.plugin.api.VariableProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
* This class provides a way to access various storage readers by their bean names from plugins.
**/
@Component
@RequiredArgsConstructor
public class NamedBeansVariableProvider implements VariableProvider {

    private final ApplicationContext applicationContext;
    private volatile Map<String, Object> cachedVariables;

    @Override
    public Map<String, Object> getVariables() {
        if (cachedVariables == null) {
            synchronized (this) {
                if (cachedVariables == null) {
                    cachedVariables = initVariables();
                }
            }
        }
        return cachedVariables;
    }

    private Map<String, Object> initVariables() {
        Map<String, Object> variables = new HashMap<>();

        addBean(variables, "asset_reader", "assetStorageReader");
        addBean(variables, "block_reader", "blockStorageReader");
        addBean(variables, "epoch_param_reader", "epochParamStorage");
        addBean(variables, "voting_procedure_reader", "votingProcedureStorageReader");
        addBean(variables, "delegation_vote_reader", "delegationVoteStorageReader");
        addBean(variables, "committee_reader", "committeeStorageReader");
        addBean(variables, "gov_action_reader", "govActionProposalStorageReader");
        addBean(variables, "constitution_reader", "constitutionStorageReader");
        addBean(variables, "metadata_reader", "txMetadataStorageReader");
        addBean(variables, "mir_reader", "mirStorageReader");
        addBean(variables, "datum_reader", "datumStorageReader");
        addBean(variables, "script_reader", "scriptStorageReader");
        addBean(variables, "tx_script_reader", "txScriptStorageReader");
        addBean(variables, "pool_cert_reader", "poolCertificateStorageReader");
        addBean(variables, "pool_reader", "poolStorageReader");
        addBean(variables, "staking_cert_reader", "stakingStorageReader");
        addBean(variables, "txn_reader", "transactionStorageReader");
        addBean(variables, "txn_witness_reader", "transactionWitnessStorageReader");
        addBean(variables, "withdrawal_reader", "withdrawalStorageReader");
        addBean(variables, "utxo_reader", "utxoStorageReader");
        addBean(variables, "drep_reader", "dRepStorageReader");
        addBean(variables, "proposal_status_reader", "govActionProposalStatusStorageReader");

        return Collections.unmodifiableMap(variables);
    }

    private void addBean(Map<String, Object> variables, String key, String beanName) {
        try {
            variables.put(key, applicationContext.getBean(beanName));
        } catch (NoSuchBeanDefinitionException ignored) {
            // Bean not available — skip
        }
    }
}


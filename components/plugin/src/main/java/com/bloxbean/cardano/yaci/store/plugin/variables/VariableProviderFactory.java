package com.bloxbean.cardano.yaci.store.plugin.variables;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class VariableProviderFactory {
    private List<VariableProvider> variableProviders;
    private List<VariableProvider> configuredVariableProviders;

    private Map<String, Object> variables;

    public VariableProviderFactory(List<VariableProvider> variableProviders) {
        this.variableProviders = variableProviders;
    }

    public void setConfiguredVariableProviders(List<VariableProvider> configuredVariableProviders) {
        if (this.configuredVariableProviders != null) {
            throw new IllegalStateException("Configured variable providers are already set. Cannot set again.");
        }

        this.configuredVariableProviders = configuredVariableProviders;
    }

    public Map<String, Object> getVariables() {
        if (variables == null) {
            variables = new ConcurrentHashMap<>();

            if (variableProviders != null) { // Variable providers automatically registered
                for (VariableProvider variableProvider : variableProviders) {
                    variables.putAll(variableProvider.getVariables());
                }
            }

            if (configuredVariableProviders != null) { // Variable providers configured via plugin yml
                for (VariableProvider variableProvider : configuredVariableProviders) {
                    variables.putAll(variableProvider.getVariables());
                }
            }
        }

        return variables;
    }
}

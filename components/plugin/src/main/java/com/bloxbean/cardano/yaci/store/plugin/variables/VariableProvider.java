package com.bloxbean.cardano.yaci.store.plugin.variables;

import java.util.Map;

/**
 * Implement this interface to provide variables for use in plugins.
 */
public interface VariableProvider {
    Map<String, Object> getVariables();
}

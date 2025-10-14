package com.bloxbean.cardano.yaci.store.plugin.scheduler;

import java.util.HashMap;
import java.util.Map;

/**
 * Thread-local context for scheduler variables.
 * Provides a way to inject variables into the execution context of scheduler plugins.
 */
public class SchedulerVariableContext {
    
    private static final ThreadLocal<Map<String, Object>> CONTEXT = ThreadLocal.withInitial(HashMap::new);
    
    /**
     * Set variables in the current thread's context
     */
    public static void setVariables(Map<String, Object> variables) {
        if (variables != null) {
            CONTEXT.set(new HashMap<>(variables));
        }
    }
    
    /**
     * Get a variable from the current thread's context
     */
    public static Object getVariable(String name) {
        Map<String, Object> variables = CONTEXT.get();
        return variables != null ? variables.get(name) : null;
    }
    
    /**
     * Get all variables from the current thread's context
     */
    public static Map<String, Object> getVariables() {
        return new HashMap<>(CONTEXT.get());
    }
    
    /**
     * Clear variables from the current thread's context
     */
    public static void clearVariables() {
        CONTEXT.remove();
    }
}
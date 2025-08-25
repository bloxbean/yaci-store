package com.bloxbean.cardano.yaci.store.plugin.polyglot.common;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for polyglot plugin developers to convert complex objects
 * before storing them in global state. This prevents "Context is already closed"
 * exceptions when other plugins try to access these objects after the GraalVM
 * context is closed.
 * 
 * Usage in Python plugins:
 * <pre>
 * from com.bloxbean.cardano.yaci.store.plugin.polyglot.common import PolyglotConverter
 * 
 * # Convert before storing in global state
 * my_dict = {"key": "value", "nested": {"data": 123}}
 * global_state.put("shared_data", PolyglotConverter.unwrap(my_dict))
 * </pre>
 * 
 * Usage in JavaScript plugins:
 * <pre>
 * const PolyglotConverter = Java.type('com.bloxbean.cardano.yaci.store.plugin.polyglot.common.PolyglotConverter');
 * 
 * // Convert before storing in global state
 * const myObj = {key: "value", nested: {data: 123}};
 * global_state.put("shared_data", PolyglotConverter.unwrap(myObj));
 * </pre>
 */
@Slf4j
public class PolyglotConverter {
    
    /**
     * Unwraps a polyglot value (Python dict, JavaScript object, etc.) to a 
     * native Java object that can be safely stored in global state and accessed
     * by other plugins after the original context is closed.
     * 
     * @param value The value to unwrap (can be a GraalVM Value, proxy object, or regular Java object)
     * @return A native Java object (HashMap for dicts/objects, ArrayList for lists/arrays, etc.)
     */
    public static Object unwrap(Object value) {
        Object converted = ValueConverter.convertToJava(value);
        
        if (log.isDebugEnabled()) {
            log.debug("Unwrapped {} to {}", 
                value != null ? value.getClass().getSimpleName() : "null",
                converted != null ? converted.getClass().getSimpleName() : "null");
        }
        
        return converted;
    }
    
    /**
     * Checks if a value needs to be unwrapped before storing in global state.
     * Returns true if the value is a GraalVM proxy object that will fail
     * when accessed after context closure.
     * 
     * @param value The value to check
     * @return true if unwrapping is needed, false otherwise
     */
    public static boolean needsUnwrap(Object value) {
        if (value == null) {
            return false;
        }
        
        String className = value.getClass().getName();
        
        // Check for GraalVM proxy objects
        if (className.contains("com.oracle.truffle") || 
            className.contains("org.graalvm.polyglot")) {
            return true;
        }
        
        // Check for proxy Maps/Lists that aren't standard Java collections
        if (value instanceof java.util.Map && 
            !(value instanceof java.util.HashMap) && 
            !(value instanceof java.util.concurrent.ConcurrentHashMap)) {
            return true;
        }
        
        if (value instanceof java.util.List && 
            !(value instanceof java.util.ArrayList) && 
            !(value instanceof java.util.LinkedList)) {
            return true;
        }
        
        return false;
    }
}
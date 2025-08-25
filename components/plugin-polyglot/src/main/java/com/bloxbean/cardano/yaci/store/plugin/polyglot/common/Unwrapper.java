package com.bloxbean.cardano.yaci.store.plugin.polyglot.common;

/**
 * A simple wrapper to provide the unwrap functionality to polyglot scripts.
 * This allows scripts to call unwrap(value) directly without needing to reference
 * the PolyglotConverter class.
 * 
 * Usage in Python:
 * <pre>
 * my_dict = {"key": "value", "nested": {"data": 123}}
 * state.put("shared_data", unwrap(my_dict))
 * </pre>
 * 
 * Usage in JavaScript:
 * <pre>
 * const myObj = {key: "value", nested: {data: 123}};
 * state.put("shared_data", unwrap(myObj));
 * </pre>
 */
public class Unwrapper {
    
    /**
     * Unwraps a polyglot value to a native Java object.
     * This is a delegate to PolyglotConverter.unwrap()
     */
    public Object unwrap(Object value) {
        return PolyglotConverter.unwrap(value);
    }
    
    /**
     * Checks if a value needs to be unwrapped.
     * This is a delegate to PolyglotConverter.needsUnwrap()
     */
    public boolean needsUnwrap(Object value) {
        return PolyglotConverter.needsUnwrap(value);
    }
}
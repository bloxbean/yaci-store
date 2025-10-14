package com.bloxbean.cardano.yaci.store.plugin.polyglot.common;

import org.graalvm.polyglot.Value;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Utility class to convert GraalVM Value objects to native Java objects.
 * This is necessary to prevent "Context is already closed" exceptions when
 * accessing complex objects stored by polyglot scripts after context closure.
 */
@Slf4j
public class ValueConverter {

    /**
     * Converts a GraalVM Value object to a native Java object.
     * Handles nested structures recursively.
     * 
     * @param value The GraalVM Value to convert
     * @return The converted Java object, or the original value if not a GraalVM Value
     */
    public static Object convertToJava(Object value) {
        if (!(value instanceof Value)) {
            // Not a GraalVM Value, return as-is
            if (log.isTraceEnabled()) {
                log.trace("Value is not a GraalVM Value, returning as-is: {}", value.getClass().getName());
            }
            return value;
        }

        Value graalValue = (Value) value;
        if (log.isDebugEnabled()) {
            log.debug("Converting GraalVM Value to Java object");
        }
        
        try {
            // Handle null
            if (graalValue.isNull()) {
                return null;
            }
            
            // Handle primitives - these usually auto-convert but let's be explicit
            if (graalValue.isBoolean()) {
                return graalValue.asBoolean();
            }
            if (graalValue.isNumber()) {
                if (graalValue.fitsInInt()) {
                    return graalValue.asInt();
                } else if (graalValue.fitsInLong()) {
                    return graalValue.asLong();
                } else if (graalValue.fitsInDouble()) {
                    return graalValue.asDouble();
                } else {
                    // BigInteger or other number type
                    return graalValue.as(Number.class);
                }
            }
            if (graalValue.isString()) {
                return graalValue.asString();
            }
            
            // Handle arrays/lists
            if (graalValue.hasArrayElements()) {
                List<Object> list = new ArrayList<>();
                long size = graalValue.getArraySize();
                for (long i = 0; i < size; i++) {
                    Value element = graalValue.getArrayElement(i);
                    list.add(convertToJava(element));
                }
                return list;
            }
            
            // Handle objects/maps
            if (graalValue.hasMembers()) {
                Map<String, Object> map = new HashMap<>();
                for (String key : graalValue.getMemberKeys()) {
                    Value member = graalValue.getMember(key);
                    // Skip function members
                    if (member != null && !member.canExecute()) {
                        map.put(key, convertToJava(member));
                    }
                }
                return map;
            }
            
            // Handle dates
            if (graalValue.isDate()) {
                return graalValue.asDate();
            }
            if (graalValue.isTime()) {
                return graalValue.asTime();
            }
            if (graalValue.isInstant()) {
                return graalValue.asInstant();
            }
            
            // If we can't convert it, try to get it as a generic object
            // This might still fail if the context is closed
            if (log.isDebugEnabled()) {
                log.debug("Unable to convert GraalVM Value of type: {}, attempting generic conversion", 
                    graalValue.getMetaObject() != null ? graalValue.getMetaObject().toString() : "unknown");
            }
            
            return graalValue.as(Object.class);
            
        } catch (Exception e) {
            log.warn("Failed to convert GraalVM Value to Java object: {}", e.getMessage());
            // Return the original value if conversion fails
            return value;
        }
    }
    
    /**
     * Checks if a value needs conversion (i.e., is a GraalVM Value with complex type)
     * 
     * @param value The value to check
     * @return true if the value needs conversion, false otherwise
     */
    public static boolean needsConversion(Object value) {
        if (!(value instanceof Value)) {
            return false;
        }
        
        Value graalValue = (Value) value;
        
        // Primitives usually auto-convert, but complex types need explicit conversion
        return graalValue.hasArrayElements() || graalValue.hasMembers();
    }
}
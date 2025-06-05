package com.bloxbean.cardano.yaci.store.plugin.polyglot.common.pool;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.KeyedPooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.graalvm.polyglot.Context;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class PolyglotContextPoolFactory implements KeyedPooledObjectFactory<String, Context> {
    private static ConcurrentHashMap<String, ContextSupplier> contextSuppliers = new ConcurrentHashMap<>();

    public static void addContextSupplier(String key, ContextSupplier contextSupplier) {
        contextSuppliers.put(key, contextSupplier);
    }

    @Override
    public PooledObject<Context> makeObject(String key) throws Exception {

        if (!contextSuppliers.containsKey(key)) {
            throw new IllegalArgumentException("No context supplier found for key: " + key);
        }

        // Create a new context based on the key
        ContextSupplier contextSupplier = contextSuppliers.get(key);

        var context = contextSupplier.createContext();
        return new DefaultPooledObject<>(context);
    }

    @Override
    public void destroyObject(String key, PooledObject<Context> p) throws Exception {
        Context ctx = p.getObject();
        if (ctx != null ) {
            ctx.close();
        }
    }

    @Override
    public boolean validateObject(String key, PooledObject<Context> p) {
        Context ctx = p.getObject();

        if (ctx == null)
            return false;

        //Validate by entering and exiting the context
        try {
            ctx.enter();
            ctx.leave();
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    @Override
    public void activateObject(String key, PooledObject<Context> p) throws Exception {
        // nothing extra to do when this context is borrowed
    }

    @Override
    public void passivateObject(String key, PooledObject<Context> p) throws Exception {
        // nothing extra to do when this context is returned
    }
}

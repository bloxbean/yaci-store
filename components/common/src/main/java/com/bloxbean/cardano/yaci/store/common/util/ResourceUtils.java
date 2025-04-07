package com.bloxbean.cardano.yaci.store.common.util;

import java.io.FileInputStream;
import java.io.InputStream;

public class ResourceUtils {

    /**
     * Load an InputStream from a filename string. Supports "classpath:" or absolute/relative file paths.
     *
     * @param location resource path, e.g., "classpath:genesis.json" or "/etc/genesis.json"
     * @return InputStream for the resource
     * @throws Exception if the resource cannot be found
     */
    public static InputStream openInputStream(String location) throws Exception {
        if (location.startsWith("classpath:")) {
            String resourcePath = location.substring("classpath:".length());
            InputStream is = Thread.currentThread()
                    .getContextClassLoader()
                    .getResourceAsStream(resourcePath);
            if (is == null) {
                throw new IllegalArgumentException("Classpath resource not found: " + resourcePath);
            }
            return is;
        } else {
            return new FileInputStream(location);
        }
    }
}


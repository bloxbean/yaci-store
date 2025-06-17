package com.bloxbean.cardano.yaci.store.plugin.http;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

class URIEncoder {
    public static String encode(String val) {
        return URLEncoder.encode(val, StandardCharsets.UTF_8);
    }
}


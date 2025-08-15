package com.bloxbean.cardano.yaci.store.plugin.util;

import com.bloxbean.cardano.yaci.store.plugin.file.PluginFileClient;
import com.bloxbean.cardano.yaci.store.plugin.http.PluginHttpClient;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class PluginContextUtil {

    public final JdbcTemplate jdbc;
    public final NamedParameterJdbcTemplate namedJdbc;
    public final RestTemplate rest;
    public final Environment env;
    public final PluginHttpClient http;
    public final PluginFileClient files;
    public final Locker locker;

    public JdbcTemplate getJdbc() {
        return jdbc;
    }

    public NamedParameterJdbcTemplate getNamedJdbc() {
        return namedJdbc;
    }

    public RestTemplate getRest() {
        return rest;
    }

    public Environment getEnv() {
        return env;
    }

    public PluginHttpClient getHttp() {
        return http;
    }

    public PluginFileClient getFiles() {
        return files;
    }

    public Locker getLocker() {
        return locker;
    }
}

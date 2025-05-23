package com.bloxbean.cardano.yaci.store.plugin.util;

import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class PluginContextUtil {

    private final JdbcTemplate jdbcTemplate;
    private final RestTemplate restTemplate;
    private final Environment environment;

    public JdbcTemplate getJdbc() {
        return jdbcTemplate;
    }

    public RestTemplate getRest() {
        return restTemplate;
    }

    public Environment getEnv() {
        return environment;
    }

}

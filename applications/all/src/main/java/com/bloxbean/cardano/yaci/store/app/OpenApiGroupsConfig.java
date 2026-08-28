package com.bloxbean.cardano.yaci.store.app;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Splits the OpenAPI documentation into separate Swagger UI "definitions" so that the
 * optional API families do not clutter the core API list:
 *
 * <ul>
 *   <li><b>core-apis</b> — every documented endpoint except the groups below</li>
 *   <li><b>analytics</b> — the analytics query and admin APIs ({@code /api/v1/analytics/**})</li>
 *   <li><b>blockfrost</b> — the Blockfrost-compatible API ({@code ${blockfrost.apiPrefix}/**})</li>
 * </ul>
 *
 * <p>Swagger UI shows the groups in its "Select a definition" dropdown; each is also served
 * as its own spec at {@code /v3/api-docs/{group}}. A group whose endpoints are not enabled
 * (e.g. Blockfrost or analytics disabled) simply renders empty. The title/version/license from
 * {@link YaciStoreApplication#customOpenAPI()} apply to every group.</p>
 */
@Configuration
public class OpenApiGroupsConfig {

    private static final String ANALYTICS_PATHS = "/api/v1/analytics/**";

    @Value("${blockfrost.apiPrefix:/api/v1/blockfrost}")
    private String blockfrostApiPrefix;

    @Bean
    public GroupedOpenApi coreApis() {
        return GroupedOpenApi.builder()
                .group("core-apis")
                .displayName("Core APIs")
                .pathsToMatch("/**")
                .pathsToExclude(ANALYTICS_PATHS, blockfrostPaths())
                .build();
    }

    @Bean
    public GroupedOpenApi analyticsApis() {
        return GroupedOpenApi.builder()
                .group("analytics")
                .displayName("Analytics APIs")
                .pathsToMatch(ANALYTICS_PATHS)
                .build();
    }

    @Bean
    public GroupedOpenApi blockfrostApis() {
        return GroupedOpenApi.builder()
                .group("blockfrost")
                .displayName("Blockfrost APIs")
                .pathsToMatch(blockfrostPaths())
                .build();
    }

    private String blockfrostPaths() {
        String prefix = blockfrostApiPrefix.endsWith("/")
                ? blockfrostApiPrefix.substring(0, blockfrostApiPrefix.length() - 1)
                : blockfrostApiPrefix;
        return prefix + "/**";
    }
}

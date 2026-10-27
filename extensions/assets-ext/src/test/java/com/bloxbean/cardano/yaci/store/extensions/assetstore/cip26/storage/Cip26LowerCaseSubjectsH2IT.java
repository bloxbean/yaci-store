package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** {@code V0_1700_4} on a real H2 database: the CIP-26 subjects already stored are lower-cased, so that an upgrade stays in place. */
@JdbcTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.schema-locations=classpath:db/store/h2/V0_1700_1__init.sql,classpath:db/store/h2/V0_1700_2__validation_errors.sql,"
                + "classpath:db/store/h2/V0_1700_3__invalid_token.sql"
})
@DisplayName("V0_1700_4 lower-cases CIP-26 subjects on H2")
class Cip26LowerCaseSubjectsH2IT {

    private static final String MIGRATION = "db/store/h2/V0_1700_4__cip26_lower_case_subjects.sql";

    @SpringBootConfiguration
    static class Config {
    }

    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private DataSource dataSource;

    private void store(String subject, String name, String logo) {
        jdbc.update("INSERT INTO cip26_metadata (subject, name, logo) VALUES (?, ?, ?)", subject, name, logo);
    }

    private void migrate() {
        new ResourceDatabasePopulator(new ClassPathResource(MIGRATION)).execute(dataSource);
    }

    private List<Map<String, Object>> rows() {
        return jdbc.queryForList("SELECT subject, name, logo FROM cip26_metadata ORDER BY subject");
    }

    @Test
    void lowerCasesUpperAndMixedCaseSubjectsWithTheirLogoAndKeepsTheLowerCaseTwin() {
        store("AABB01", "upper only", "logo upper");
        store("ccdd02", "lower only", null);
        store("EEFF03", "upper twin", "logo of upper twin");
        store("eeff03", "lower twin", "logo of lower twin");
        store("A1b2C3", "mixed", null);

        migrate();

        assertThat(rows()).extracting(row -> row.get("SUBJECT") + "|" + row.get("NAME") + "|" + row.get("LOGO"))
                .containsExactly(
                        "a1b2c3|mixed|null",
                        "aabb01|upper only|logo upper",
                        "ccdd02|lower only|null",
                        "eeff03|lower twin|logo of lower twin");
    }

    @Test
    void changesNothingTheSecondTimeOrOnALowerCaseDatabase() {
        store("aabb01", "lower", null);

        migrate();
        migrate();

        assertThat(rows()).extracting(row -> row.get("SUBJECT")).containsExactly("aabb01");
    }
}

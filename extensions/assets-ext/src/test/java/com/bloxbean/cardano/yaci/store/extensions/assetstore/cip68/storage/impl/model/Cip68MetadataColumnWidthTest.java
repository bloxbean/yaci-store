package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The parser drops CIP-68 strings longer than the {@code *_MAX_LENGTH} constants so the insert can't
 * fail and stop the sync. Flyway, not {@code @Column(length)}, creates the columns, so this pins the
 * constants to the actual {@code VARCHAR(n)} widths in every dialect's migration.
 */
class Cip68MetadataColumnWidthTest {

    @ParameterizedTest
    @ValueSource(strings = {"h2", "mysql", "postgresql"})
    void maxLengthConstantsMatchMigrationColumnWidths(String dialect) throws IOException {
        String table = cip68TableDefinition(dialect);

        assertThat(varcharWidth(table, "name")).isEqualTo(Cip68Metadata.NAME_MAX_LENGTH);
        assertThat(varcharWidth(table, "ticker")).isEqualTo(Cip68Metadata.TICKER_MAX_LENGTH);
        assertThat(varcharWidth(table, "url")).isEqualTo(Cip68Metadata.URL_MAX_LENGTH);
        assertThat(varcharWidth(table, "media_type")).isEqualTo(Cip68Metadata.MEDIA_TYPE_MAX_LENGTH);
    }

    private static String cip68TableDefinition(String dialect) throws IOException {
        String path = "db/store/" + dialect + "/V0_1700_1__init.sql";
        try (InputStream in = Cip68MetadataColumnWidthTest.class.getClassLoader().getResourceAsStream(path)) {
            assertThat(in).as(path).isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            Matcher table = Pattern.compile("CREATE TABLE cip68_metadata \\((.*?)\\n\\);", Pattern.DOTALL).matcher(sql);
            assertThat(table.find()).as("cip68_metadata in " + path).isTrue();
            return table.group(1);
        }
    }

    private static int varcharWidth(String tableDefinition, String column) {
        Matcher width = Pattern.compile("(?m)^\\s*`?" + column + "`?\\s+VARCHAR\\((\\d+)\\)").matcher(tableDefinition);
        assertThat(width.find()).as("VARCHAR column " + column).isTrue();
        return Integer.parseInt(width.group(1));
    }

}

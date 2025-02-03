package com.bloxbean.cardano.yaci.store.dbutils.index.util;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Schema;
import org.jooq.Table;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

@Component
@AllArgsConstructor
@Slf4j
public class DatabaseUtils {
    public enum DbType {postgres, mysql, h2}

    private final JdbcTemplate jdbcTemplate;
    private final DSLContext dsl;

    /**
     * Checks if a table exists in the current schema/database.
     *
     * @param tableName The name of the table to check.
     * @return true if the table exists; false otherwise.
     */
    public boolean tableExists(String tableName) {
        String databaseProductName = getDatabaseProductName();
        String adjustedTableName = adjustTableNameCase(tableName, databaseProductName);
        String currentSchema = getCurrentSchema();

        String sql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = ? AND table_schema = ?";

        Integer count = jdbcTemplate.queryForObject(sql, new Object[]{adjustedTableName, currentSchema}, Integer.class);
        return count != null && count > 0;
    }

    /**
     * Checks whether a specific index exists on a given table in the current database schema.
     *
     * @param tableName The name of the table to check for the index.
     * @param index The name of the index to check for existence.
     * @return true if the specified index exists on the given table; false otherwise.
     */
    public boolean indexExists(String tableName, String index) {
        String databaseProductName = getDatabaseProductName();
        String adjustedTableName = adjustTableNameCase(tableName, databaseProductName);

        String currentSchema = getCurrentSchema();
        Schema schema = dsl.meta()
                .getSchemas()
                .stream()
                .filter(s -> s.getName().equalsIgnoreCase(currentSchema)) // Match by schema name
                .findFirst()
                .orElse(null);

        if (schema == null) {
            return false;
        }

        Table<?> table = schema.getTable(adjustedTableName);
        if (table == null) {
            return false;
        }

        // Check if the specific index exists for this table
        return table.getIndexes()
                .stream()
                .anyMatch(idx -> idx.getName().equalsIgnoreCase(index));
    }

    /**
     * Retrieves the database product name (e.g., PostgreSQL, MySQL, H2).
     *
     * @return The database product name.
     */
    private String getDatabaseProductName() {
        return jdbcTemplate.execute((Connection conn) -> conn.getMetaData().getDatabaseProductName());
    }

    /**
     * Retrieves the current schema or database name.
     *
     * @return The current schema or database name.
     */
    private String getCurrentSchema() {
        var dbType = getDbType(jdbcTemplate.getDataSource()).orElse(null);

        if (dbType == null)
            return null;

        String sql = null;
        switch (dbType) {
            case DbType.postgres:
                sql = "SELECT CURRENT_SCHEMA()";
                break;
            case DbType.mysql:
                sql = "SELECT DATABASE()";
                break;
            case DbType.h2:
                sql = "SELECT CURRENT_SCHEMA()";
                break;
            default:
                throw new UnsupportedOperationException("Unsupported database: " + dbType);
        }

        return jdbcTemplate.queryForObject(sql, String.class);
    }

    /**
     * Adjusts the table name casing based on the database's identifier handling.
     *
     * @param tableName           The original table name.
     * @param databaseProductName The name of the database product.
     * @return The adjusted table name.
     */
    private String adjustTableNameCase(String tableName, String databaseProductName) {
        if (databaseProductName.equalsIgnoreCase("H2")) {
            return tableName.toUpperCase();
        } else {
            // PostgreSQL and MySQL generally use lowercase (unless quoted)
            return tableName.toLowerCase();
        }
    }

    public static Optional<DbType> getDbType(DataSource dataSource) {
        String url = null;
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            url = connection.getMetaData().getURL();
            if (url.contains("postgresql")) {
                return Optional.of(DbType.postgres);
            }
            if (url.contains("mysql")) {
                return Optional.of(DbType.mysql);
            }
            if (url.contains("h2")) {
                return Optional.of(DbType.h2);
            }

        } catch (Exception e) {
            log.error("Invalid or unsupported database type : " + url);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {

                }
            }
        }
        return Optional.empty();
    }

}

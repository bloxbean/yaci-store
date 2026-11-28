package com.bloxbean.cardano.yaci.store.snapshot.convert;

/**
 * A named, reusable transformation from a source Parquet column to a PostgreSQL target column,
 * expressed as a DuckDB SQL expression.
 *
 * <p>Named converters rather than inline expressions in YAML: they are type-checked once, tested
 * once, documented once, and every table that uses one behaves identically. A specification
 * therefore never carries an executable SQL fragment.
 */
public interface ValueConverter {

    String name();

    /**
     * @param sourceExpression already-quoted reference to the source column
     * @return a DuckDB SQL expression producing the target value
     */
    String toSql(String sourceExpression);

    /**
     * Reject a mapping that cannot be right, at specification-validation time.
     *
     * @param sourceType DuckLake source type, lowercase
     * @param targetType PostgreSQL target type, lowercase
     * @return {@code null} when acceptable, otherwise the reason it is rejected
     */
    default String reject(String sourceType, String targetType) {
        return null;
    }
}

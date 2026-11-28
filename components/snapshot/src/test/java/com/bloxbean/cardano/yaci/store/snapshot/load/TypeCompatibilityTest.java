package com.bloxbean.cardano.yaci.store.snapshot.load;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The type vocabulary this schema actually uses.
 *
 * <p>The source names come from the DuckLake catalog of the preprod analytics export and the target
 * names from a migrated PostgreSQL schema, so an unclassified type cannot slip through and fail
 * halfway into a multi-hour import.
 */
class TypeCompatibilityTest {

    @Test
    void acceptsEveryPairTheOperationalSchemaActuallyUses() {
        assertThat(TypeCompatibility.reject("varchar", "character varying(64)")).isNull();
        assertThat(TypeCompatibility.reject("varchar", "text")).isNull();
        assertThat(TypeCompatibility.reject("int16", "smallint")).isNull();
        assertThat(TypeCompatibility.reject("int32", "integer")).isNull();
        assertThat(TypeCompatibility.reject("int64", "bigint")).isNull();
        assertThat(TypeCompatibility.reject("int32", "bigint")).isNull();
        assertThat(TypeCompatibility.reject("decimal(38,0)", "numeric(38,0)")).isNull();
        assertThat(TypeCompatibility.reject("decimal(20,0)", "numeric(20,0)")).isNull();
        assertThat(TypeCompatibility.reject("float64", "double precision")).isNull();
        assertThat(TypeCompatibility.reject("boolean", "boolean")).isNull();
        assertThat(TypeCompatibility.reject("date", "date")).isNull();
        assertThat(TypeCompatibility.reject("uuid", "uuid")).isNull();
        assertThat(TypeCompatibility.reject("timestamptz", "timestamp without time zone")).isNull();
    }

    @Test
    void rejectsTheConversionsThatNeedANamedConverter() {
        assertThat(TypeCompatibility.reject("timestamptz", "bigint"))
                .contains("not assignable");
        assertThat(TypeCompatibility.reject("varchar", "jsonb"))
                .contains("not assignable");
        assertThat(TypeCompatibility.reject("int64", "integer"))
                .contains("can overflow");
        assertThat(TypeCompatibility.reject("int64", "smallint"))
                .contains("can overflow");
    }

    @Test
    void rejectsAnUnknownTypeRatherThanGuessing() {
        assertThat(TypeCompatibility.reject("geography", "text")).contains("unsupported type pair");
        assertThat(TypeCompatibility.reject("varchar", null)).contains("unknown type");
    }

    @Test
    void normalisesPostgresqlSpellings() {
        assertThat(TypeCompatibility.normalize("character varying(500)")).isEqualTo("varchar");
        assertThat(TypeCompatibility.normalize("timestamp without time zone")).isEqualTo("timestamp");
        assertThat(TypeCompatibility.normalize("double precision")).isEqualTo("double");
        assertThat(TypeCompatibility.normalize("numeric(38,0)")).isEqualTo("numeric(38,0)");
    }
}

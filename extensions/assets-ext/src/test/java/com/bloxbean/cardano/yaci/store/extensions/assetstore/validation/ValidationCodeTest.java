package com.bloxbean.cardano.yaci.store.extensions.assetstore.validation;

import io.swagger.v3.oas.annotations.media.Schema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ValidationCode")
class ValidationCodeTest {

    private static List<String> codes() {
        return Arrays.stream(ValidationCode.class.getDeclaredFields())
                .filter(f -> Modifier.isPublic(f.getModifiers()) && Modifier.isStatic(f.getModifiers()) && f.getType() == String.class)
                .filter(f -> !f.getName().equals("DOCUMENTATION"))
                .map(ValidationCodeTest::valueOf)
                .toList();
    }

    private static String valueOf(Field f) {
        try {
            return (String) f.get(null);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    @DisplayName("every code is described in the OpenAPI documentation")
    void everyCodeIsDocumented() {
        assertThat(codes()).isNotEmpty().allSatisfy(code ->
                assertThat(ValidationCode.DOCUMENTATION).as(code).contains("| `" + code + "` |"));
    }

    @Test
    @DisplayName("the documentation describes no code that does not exist")
    void noInventedCodes() {
        long rows = ValidationCode.DOCUMENTATION.lines().filter(l -> l.startsWith("| `")).count();
        assertThat(rows).isEqualTo(codes().size());
    }

    @Test
    @DisplayName("every code is an allowed value of the code field of a validation error")
    void everyCodeIsAnAllowedValue() throws Exception {
        Schema schema = Arrays.stream(ValidationError.class.getRecordComponents())
                .filter(c -> c.getName().equals("code")).findFirst().orElseThrow()
                .getAccessor().getAnnotation(Schema.class);
        assertThat(schema).isNotNull();
        assertThat(schema.allowableValues()).containsExactlyInAnyOrderElementsOf(codes());
    }

    @Test
    @DisplayName("the codes of the validators and the parser are the shared ones")
    void validatorsUseTheSharedCodes() {
        assertThat(com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.parser.Cip68DatumParser.TOO_DEEP).isEqualTo(ValidationCode.TOO_DEEP);
        assertThat(com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.service.Cip68Validator.INVALID_URI_SCHEME)
                .isEqualTo(ValidationCode.INVALID_URI_SCHEME);
        assertThat(com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.util.Cip26MetadataValidator.INVALID_VALUE)
                .isEqualTo(ValidationCode.INVALID_VALUE);
    }
}

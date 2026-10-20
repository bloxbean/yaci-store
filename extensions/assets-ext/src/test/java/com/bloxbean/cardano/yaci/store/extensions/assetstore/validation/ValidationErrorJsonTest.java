package com.bloxbean.cardano.yaci.store.extensions.assetstore.validation;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.Metadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.Subject;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.cip26.Cip26TokenMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.model.Cip26Metadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.FungibleTokenMetadata;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The shape of {@code validation_errors} on the wire and in the JSON column: snake_case names, a {@code sub_standard} only
 * where there is one, and an array that is always there, empty when a token follows its CIP.
 */
class ValidationErrorJsonTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private static final ValidationError ERROR = ValidationError.mandatory("CIP-68", "333", "description", "missing_value",
            "description field is missing a mandatory value");

    @Test
    void aValidationErrorIsWrittenInSnakeCase() throws Exception {
        JsonNode json = mapper.readTree(mapper.writeValueAsString(ERROR));

        assertThat(json.get("cip").asText()).isEqualTo("CIP-68");
        assertThat(json.get("sub_standard").asText()).isEqualTo("333");
        assertThat(json.get("field_name").asText()).isEqualTo("description");
        assertThat(json.get("field_category").asText()).isEqualTo("MANDATORY");
        assertThat(json.get("code").asText()).isEqualTo("missing_value");
        assertThat(json.get("error").asText()).isEqualTo("description field is missing a mandatory value");
        assertThat(json.fieldNames()).toIterable().doesNotContain("subStandard", "fieldName", "fieldCategory");
    }

    @Test
    void theSubStandardIsLeftOutWhenThereIsNone() throws Exception {
        ValidationError cip26 = ValidationError.optional("CIP-26", null, "ticker", "invalid_length", "ticker is too long");

        assertThat(mapper.readTree(mapper.writeValueAsString(cip26)).has("sub_standard")).isFalse();
    }

    @Test
    void aListOfErrorsSurvivesTheRoundTripThroughTheJsonColumn() throws Exception {
        String json = mapper.writeValueAsString(List.of(ERROR));

        List<ValidationError> back = mapper.readValue(json, new TypeReference<>() { });

        assertThat(back).containsExactly(ERROR);
    }

    @Test
    void aSubjectAlwaysHasAValidationErrorsArray() throws Exception {
        JsonNode withErrors = mapper.readTree(mapper.writeValueAsString(
                new Subject("s", Metadata.empty(), null, List.of(ERROR))));
        JsonNode without = mapper.readTree(mapper.writeValueAsString(new Subject("s", Metadata.empty(), null)));

        assertThat(withErrors.get("validation_errors")).hasSize(1);
        assertThat(without.get("validation_errors").isArray()).isTrue();
        assertThat(without.get("validation_errors")).isEmpty();
        assertThat(without.has("validationErrors")).isFalse();
    }

    @Test
    void theCip68DtoWritesValidationErrorsInSnakeCase() throws Exception {
        JsonNode json = mapper.readTree(mapper.writeValueAsString(
                new FungibleTokenMetadata(null, null, null, "N", null, null, 1L, List.of(ERROR))));

        assertThat(json.get("validation_errors")).hasSize(1);
        assertThat(json.get("validation_errors").get(0).get("sub_standard").asText()).isEqualTo("333");
    }

    @Test
    void theCip26DtoWritesAnEmptyArrayWhenThereAreNoErrors() throws Exception {
        JsonNode json = mapper.readTree(mapper.writeValueAsString(Cip26TokenMetadata.builder().subject("s").build()));

        assertThat(json.get("validation_errors").isArray()).isTrue();
        assertThat(json.get("validation_errors")).isEmpty();
    }

    @Test
    void theCip26EntityReturnedByTheEndpointUsesTheSameName() throws Exception {
        Cip26Metadata entity = new Cip26Metadata();
        entity.setSubject("s");
        entity.setValidationErrors(List.of(ValidationError.mandatory("CIP-26", null, "name", "missing_value", "e")));

        JsonNode json = mapper.readTree(mapper.writeValueAsString(entity));

        assertThat(json.get("validation_errors")).hasSize(1);
        assertThat(json.has("validationErrors")).isFalse();
    }
}

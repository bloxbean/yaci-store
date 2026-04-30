package com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.cip26;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.cip26.wellknownproperties.DecimalsProperty;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.cip26.wellknownproperties.DescriptionProperty;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.cip26.wellknownproperties.LogoProperty;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.cip26.wellknownproperties.NameProperty;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.cip26.wellknownproperties.TickerProperty;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.cip26.wellknownproperties.UrlProperty;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.model.Item;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.model.Mapping;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.model.Signature;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * CIP-26 metadata DTO returned under {@code standards.cip26} when {@code show_cips_details=true}.
 * <p>
 * Shape ported 1:1 from {@code cf-token-metadata-registry}'s
 * {@code api.model.rest.TokenMetadata} so yaci-store and the CF V2 API produce
 * byte-for-byte equivalent JSON for this block. Each well-known property is wrapped
 * in a {@link TokenMetadataProperty} envelope ({@code value}/{@code signatures}/{@code sequenceNumber})
 * — the data already lives in {@code ft_offchain_metadata.properties} (the original
 * CIP-26 mapping JSON), so we just lift it through.
 * <p>
 * Named {@code Cip26TokenMetadata} (not {@code TokenMetadata}) to avoid colliding with
 * the JPA entity in {@code cip26.storage.impl.model.TokenMetadata}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Cip26TokenMetadata {

    @JsonProperty("subject")
    @Schema(name = "subject", requiredMode = Schema.RequiredMode.REQUIRED)
    private String subject;

    @JsonProperty("policy")
    @Schema(name = "policy")
    private String policy;

    @JsonProperty("name")
    @Valid
    @Schema(name = "name", requiredMode = Schema.RequiredMode.REQUIRED)
    private NameProperty name;

    @JsonProperty("description")
    @Valid
    @Schema(name = "description", requiredMode = Schema.RequiredMode.REQUIRED)
    private DescriptionProperty description;

    @JsonProperty("url")
    @Valid
    @Schema(name = "url")
    private UrlProperty url;

    @JsonProperty("ticker")
    @Valid
    @Schema(name = "ticker")
    private TickerProperty ticker;

    @JsonProperty("decimals")
    @Valid
    @Schema(name = "decimals")
    private DecimalsProperty decimals;

    @JsonProperty("logo")
    @Valid
    @Schema(name = "logo")
    private LogoProperty logo;

    @JsonProperty("updated")
    @Valid
    @Schema(name = "updated")
    private Date updated;

    @JsonProperty("updatedBy")
    @Valid
    @Schema(name = "updatedBy")
    private String updatedBy;

    @Builder.Default
    private Map<String, TokenMetadataProperty<?>> additionalProperties = new HashMap<>();

    @JsonAnySetter
    public void propertiesSetter(final String propertyName, final TokenMetadataProperty<?> property) {
        if (propertyName == null) {
            throw new IllegalArgumentException("propertyName cannot be null.");
        }
        final String sanitized = propertyName.trim();
        if (sanitized.isEmpty()) {
            throw new IllegalArgumentException("propertyName cannot be empty or blank.");
        }
        if (property != null) {
            this.additionalProperties.put(sanitized, property);
        } else {
            this.additionalProperties.remove(sanitized);
        }
    }

    @JsonAnyGetter
    public Map<String, TokenMetadataProperty<?>> propertiesGetter() {
        return getAdditionalProperties();
    }

    /**
     * Build a {@code Cip26TokenMetadata} from yaci's persisted CIP-26 entity.
     * <p>
     * The original CIP-26 mapping JSON (with {@code Item} envelopes carrying signatures
     * and sequence numbers) is stored verbatim on the entity's {@code properties} column,
     * so we lift the envelopes straight through. The {@code logo} comes from the separate
     * {@code TokenLogo} table — we re-wrap it using the logo's stored {@code Item} from
     * the same mapping JSON to preserve signatures, while substituting the table copy as
     * the canonical value.
     *
     * @param entity   the CIP-26 JPA entity
     * @param logoB64  base64-encoded logo string (from {@code TokenLogo}), or null
     */
    public static Cip26TokenMetadata from(
            com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.model.TokenMetadata entity,
            @Nullable String logoB64) {
        if (entity == null) {
            return null;
        }

        Mapping mapping = entity.getProperties();
        Cip26TokenMetadataBuilder builder = Cip26TokenMetadata.builder()
                .subject(entity.getSubject())
                .policy(entity.getPolicy());

        if (mapping != null) {
            builder.name(stringProperty(mapping.name(), NameProperty::new));
            builder.description(stringProperty(mapping.description(), DescriptionProperty::new));
            builder.url(stringProperty(mapping.url(), UrlProperty::new));
            builder.ticker(stringProperty(mapping.ticker(), TickerProperty::new));
            builder.decimals(decimalsProperty(mapping.decimals()));
            if (logoB64 != null) {
                // Preserve signatures from mapping.logo() but use the canonical base64 string from TokenLogo.
                builder.logo(logoProperty(mapping.logo(), logoB64));
            }
        }

        builder.updated(entity.getUpdated() != null
                ? Date.from(entity.getUpdated().atZone(ZoneId.systemDefault()).toInstant())
                : null);
        builder.updatedBy(entity.getUpdatedBy());

        return builder.build();
    }

    private static <P extends TokenMetadataProperty<String>> P stringProperty(Item item, Supplier<P> ctor) {
        if (item == null || item.value() == null) {
            return null;
        }
        P property = ctor.get();
        property.setValue(item.value());
        property.setSignatures(toSignatures(item.signatures()));
        property.setSequenceNumber(sequenceNumberOf(item));
        return property;
    }

    private static DecimalsProperty decimalsProperty(Item item) {
        if (item == null || item.value() == null) {
            return null;
        }
        BigDecimal value;
        try {
            value = new BigDecimal(item.value());
        } catch (NumberFormatException e) {
            log.warn("Invalid decimals value '{}'", item.value());
            return null;
        }
        DecimalsProperty property = new DecimalsProperty();
        property.setValue(value);
        property.setSignatures(toSignatures(item.signatures()));
        property.setSequenceNumber(sequenceNumberOf(item));
        return property;
    }

    private static LogoProperty logoProperty(@Nullable Item itemForSignatures, String logoB64) {
        LogoProperty property = new LogoProperty();
        property.setValue(logoB64);
        property.setSignatures(itemForSignatures != null
                ? toSignatures(itemForSignatures.signatures())
                : new ArrayList<>());
        property.setSequenceNumber(itemForSignatures != null
                ? sequenceNumberOf(itemForSignatures)
                : BigDecimal.ZERO);
        return property;
    }

    private static List<AnnotatedSignature> toSignatures(List<Signature> signatures) {
        if (signatures == null || signatures.isEmpty()) {
            return new ArrayList<>();
        }
        return signatures.stream()
                .map(s -> new AnnotatedSignature(s.signature(), s.publicKey()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static BigDecimal sequenceNumberOf(Item item) {
        return item.sequenceNumber() != null ? BigDecimal.valueOf(item.sequenceNumber()) : BigDecimal.ZERO;
    }
}

package com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.model.Cip26Metadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.FungibleTokenMetadata;
import jakarta.annotation.Nullable;
import lombok.Builder;

import java.util.List;

/**
 * The merged display metadata of a token.
 * <p>
 * {@code valid} says whether the token follows its CIP completely: {@code true} when its {@code validation_errors} are empty,
 * mandatory and optional fields alike. It is in the JSON where it has always been. It used to mean "name and description are both
 * there" and was the gate of the API, so it was {@code true} for every token returned; there is no gate any more, so it can be
 * {@code false}, and it is set from the errors of every standard that has the token, after the merge.
 */
@Builder(toBuilder = true)
public record Metadata(@Nullable StringProperty name, @Nullable StringProperty description,
                       @Nullable StringProperty ticker, @Nullable LongProperty decimals,
                       @Nullable StringProperty logo, @Nullable StringProperty url,
                       @Nullable LongProperty version, boolean valid) {

    /** Metadata whose validity is set later, from the errors of the token. */
    public Metadata(@Nullable StringProperty name, @Nullable StringProperty description, @Nullable StringProperty ticker,
                    @Nullable LongProperty decimals, @Nullable StringProperty logo, @Nullable StringProperty url,
                    @Nullable LongProperty version) {
        this(name, description, ticker, decimals, logo, url, version, false);
    }

    private static final Metadata EMPTY_METADATA = Metadata.builder().build();

    public static Metadata empty() {
        return EMPTY_METADATA;
    }

    public Metadata merge(Metadata that) {
        return new Metadata(name != null ? name : that.name(),
                description != null ? description : that.description(),
                ticker != null ? ticker : that.ticker(),
                decimals != null ? decimals : that.decimals(),
                logo != null ? logo : that.logo(),
                url != null ? url : that.url(),
                version != null ? version : that.version(),
                valid);
    }

    /**
     * Build a Metadata from a CIP-26 Cip26Metadata entity, applying property filtering.
     *
     * @param entity     the CIP-26 entity
     * @param logo       the logo string (from Cip26Metadata.logo), or null if not available / not requested
     * @param properties the list of properties to include; empty means all
     */
    public static Metadata from(Cip26Metadata entity, @Nullable String logo, List<String> properties) {
        StringProperty nameProp = include("name", properties) && entity.getName() != null
                ? new StringProperty(entity.getName(), QueryPriority.CIP_26.name()) : null;
        StringProperty descProp = include("description", properties) && entity.getDescription() != null
                ? new StringProperty(entity.getDescription(), QueryPriority.CIP_26.name()) : null;
        StringProperty tickerProp = include("ticker", properties) && entity.getTicker() != null
                ? new StringProperty(entity.getTicker(), QueryPriority.CIP_26.name()) : null;
        LongProperty decimalsProp = include("decimals", properties) && entity.getDecimals() != null
                ? new LongProperty(entity.getDecimals(), QueryPriority.CIP_26.name()) : null;
        StringProperty logoProp = include("logo", properties) && logo != null
                ? new StringProperty(logo, QueryPriority.CIP_26.name()) : null;
        StringProperty urlProp = include("url", properties) && entity.getUrl() != null
                ? new StringProperty(entity.getUrl(), QueryPriority.CIP_26.name()) : null;

        return new Metadata(nameProp, descProp, tickerProp, decimalsProp, logoProp, urlProp, null);
    }

    public static Metadata from(FungibleTokenMetadata cip68TokenMetadata) {
        StringProperty nameProp = cip68TokenMetadata.name() != null ? new StringProperty(cip68TokenMetadata.name(), QueryPriority.CIP_68.name()) : null;
        StringProperty descProp = cip68TokenMetadata.description() != null ? new StringProperty(cip68TokenMetadata.description(), QueryPriority.CIP_68.name()) : null;
        StringProperty tickerProp = cip68TokenMetadata.ticker() != null ? new StringProperty(cip68TokenMetadata.ticker(), QueryPriority.CIP_68.name()) : null;
        LongProperty decimalsProp = cip68TokenMetadata.decimals() != null ? new LongProperty(cip68TokenMetadata.decimals(), QueryPriority.CIP_68.name()) : null;
        StringProperty logoProp = cip68TokenMetadata.logo() != null ? new StringProperty(cip68TokenMetadata.logo(), QueryPriority.CIP_68.name()) : null;
        StringProperty urlProp = cip68TokenMetadata.url() != null ? new StringProperty(cip68TokenMetadata.url(), QueryPriority.CIP_68.name()) : null;
        LongProperty versionProp = cip68TokenMetadata.version() != null ? new LongProperty(cip68TokenMetadata.version(), QueryPriority.CIP_68.name()) : null;

        return new Metadata(nameProp, descProp, tickerProp, decimalsProp, logoProp, urlProp, versionProp);
    }

    /** True when no property has a value (the {@code valid} flag is not a property). */
    public boolean isEmpty() {
        return name == null && description == null && ticker == null && decimals == null
                && logo == null && url == null && version == null;
    }

    private static boolean include(String propertyName, List<String> properties) {
        return properties.isEmpty() || properties.contains(propertyName);
    }

}

package com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.QueryPriority;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.model.TokenMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.FungibleTokenMetadata;
import jakarta.annotation.Nullable;
import lombok.Builder;

import java.util.List;

@Builder(toBuilder = true)
public record Metadata(StringProperty name, StringProperty description, StringProperty ticker, LongProperty decimals,
                       StringProperty logo, StringProperty url, LongProperty version) {

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
                version != null ? version : that.version());
    }

    /**
     * Build a Metadata from a CIP-26 TokenMetadata entity, applying property filtering.
     *
     * @param entity     the CIP-26 entity
     * @param logo       the logo string (from TokenLogo), or null if not available / not requested
     * @param properties the list of properties to include; empty means all
     */
    public static Metadata from(TokenMetadata entity, @Nullable String logo, List<String> properties) {
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

    public boolean isEmpty() {
        return this.equals(EMPTY_METADATA);
    }

    public boolean isValid() {
        return this.name != null && this.description != null;
    }

    private static boolean include(String propertyName, List<String> properties) {
        return properties.isEmpty() || properties.contains(propertyName);
    }

}

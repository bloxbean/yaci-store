package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Mapping(String subject,
                      Item url,
                      Item name,
                      Item ticker,
                      Item decimals,
                      Item logo,
                      String policy,
                      Item description) {
}

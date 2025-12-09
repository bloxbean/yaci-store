package com.bloxbean.cardano.yaci.store.governance.domain;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.store.common.domain.BlockAwareDomain;
import com.bloxbean.cardano.yaci.store.common.util.GovUtil;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigInteger;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class GovActionProposal extends BlockAwareDomain {

    private String txHash;

    private long index;

    private int txIndex;

    private Long slot;

    private BigInteger deposit;

    private String returnAddress;

    private GovActionType type;

    private JsonNode details;

    private String anchorUrl;

    private String anchorHash;

    private Integer epoch;

    /**
     * Get CIP-129 compliant governance action ID in bech32 format.
     * This is a computed field derived from txHash and index.
     * @return The governance action ID (e.g., "gov_action1...")
     */
    public String getGovActionId() {
        return GovUtil.toGovActionIdBech32(txHash, (int) index);
    }
}

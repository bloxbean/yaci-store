package com.bloxbean.cardano.yaci.store.mcp.server.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Paginated response for address net activity aggregation.
 *
 * Contains a page of address summaries plus metadata about the query.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AddressActivityPage {

    /**
     * List of address summaries for this page
     */
    private List<AddressNetActivitySummary> addresses;

    /**
     * Total number of addresses in this page (same as addresses.size())
     */
    private Integer totalAddresses;

    /**
     * Current page number (0-based)
     */
    private Integer page;

    /**
     * Page size (max results per page)
     */
    private Integer pageSize;

    /**
     * Total number of transactions analyzed to produce these results
     */
    private Long totalTransactionsAnalyzed;

    /**
     * Start slot of the query range
     */
    private Long startSlot;

    /**
     * End slot of the query range
     */
    private Long endSlot;
}

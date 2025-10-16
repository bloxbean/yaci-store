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
 * Generic paged result with cursor-based pagination.
 *
 * Supports stable, performant pagination through large datasets using keyset pagination.
 *
 * Usage:
 * 1. Call API with cursor=null to get first page
 * 2. Use nextCursor from response to get next page
 * 3. Continue until hasMore=false
 *
 * @param <T> Type of results in the page
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PagedResult<T> {

    /**
     * List of results for this page
     */
    private List<T> results;

    /**
     * Number of results in this page
     */
    private Integer pageSize;

    /**
     * Cursor to fetch next page (Base64 encoded).
     * Pass this as 'cursor' parameter to get next page.
     * Null if no more pages available.
     */
    private String nextCursor;

    /**
     * Whether more results are available after this page
     */
    private Boolean hasMore;

    /**
     * Total number of records processed across all pages so far
     */
    private Integer totalProcessed;
}

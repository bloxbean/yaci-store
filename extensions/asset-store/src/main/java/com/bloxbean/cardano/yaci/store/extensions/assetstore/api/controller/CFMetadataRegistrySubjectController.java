package com.bloxbean.cardano.yaci.store.extensions.assetstore.api.controller;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.model.QueryPriority;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.model.v2.*;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.service.MetadataV2QueryService;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.model.ProgrammableTokenCip113;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * V2 Subjects API — multi-standard token metadata with priority-based merging.
 * <p>
 * This controller is a <b>compatibility adapter</b> ported from the
 * <a href="https://github.com/cardano-foundation/cf-token-metadata-registry">
 * Cardano Foundation Token Metadata Registry</a> (cf-token-metadata-registry).
 * It provides the same V2 API contract ({@code /api/v2/subjects/{subject}} and
 * {@code /api/v2/subjects/query}) so that existing clients of cf-token-metadata-registry can
 * switch to a yaci-store deployment by only changing the base URL — all paths, parameters,
 * and response shapes are identical.
 * <p>
 * The V2 API merges metadata from multiple CIP standards into a single response:
 * <ul>
 *   <li><b>CIP-26</b> — offchain metadata synced from the GitHub cardano-token-registry</li>
 *   <li><b>CIP-68</b> — on-chain reference NFT metadata parsed from inline datums</li>
 *   <li><b>CIP-113</b> — programmable token extensions (transfer logic scripts)</li>
 * </ul>
 *
 * <h3>Query Priority</h3>
 * When the same property (e.g. {@code name}) exists in both CIP-26 and CIP-68, the standard
 * with higher priority wins. The default order is {@code CIP_68, CIP_26} (on-chain preferred),
 * configurable per-request via the {@code query_priority} parameter or globally via
 * {@code store.extensions.asset-store.default-query-priority}.
 *
 * <h3>CIP-113 Extensions</h3>
 * If the token's policy ID is registered as a CIP-113 programmable token, the response includes
 * an {@code extensions.cip113} block with transfer logic script hashes.
 *
 * @see MetadataV2QueryService
 * @see <a href="https://github.com/cardano-foundation/cf-token-metadata-registry/blob/main/api/src/main/java/org/cardanofoundation/tokenmetadata/registry/api/controller/V2ApiController.java">
 *     Original V2ApiController in cf-token-metadata-registry</a>
 */
@RestController
@RequestMapping("/api/v2")
@ConditionalOnExpression("${store.extensions.asset-store.enabled:false}")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "V2 Subjects (CF Registry Compatible)",
        description = "Multi-standard merged metadata API — drop-in replacement for cf-token-metadata-registry V2 API")
@Slf4j
public class CFMetadataRegistrySubjectController {

    private static final List<String> ALL_PROPERTIES = List.of();

    private static final List<String> REQUIRED_PROPERTIES = List.of("name", "description");

    private final MetadataV2QueryService metadataV2QueryService;

    @Value("${store.extensions.asset-store.default-query-priority:CIP_68,CIP_26}")
    private List<QueryPriority> defaultQueryPriority;

    /**
     * Query metadata for a single subject, merging across CIP standards by priority.
     *
     * @param subject        the subject identifier (policyId + hex assetName)
     * @param properties     optional list of properties to include (if omitted, all are returned;
     *                       when filtering, {@code name} and {@code description} are always required)
     * @param priorities     optional CIP priority override (default: {@code CIP_68, CIP_26})
     * @param showCipsDetails whether to include raw per-standard metadata in the response
     * @return the merged subject with metadata and extensions, or 404 if not found
     */
    @Operation(operationId = "getSubject", summary = "Query either all or a subset of properties of a given subject",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Merged metadata found"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Required properties (name, description) missing from filter"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Subject not found or has no valid metadata")
            })
    @GetMapping(path = "/subjects/{subject}", produces = {"application/json;charset=utf-8"})
    public ResponseEntity<Response> getSubject(
            @Parameter(description = "the concatenation of policy id and asset name (if any) to query")
            @PathVariable("subject") String subject,
            @Parameter(description = "the list of properties to be returned in the response, if none specified, all properties will be returned")
            @RequestParam(value = "property", required = false) List<String> properties,
            @Parameter(description = "the CIP priority: if the same property is present in multiple standards, the one with highest priority is returned")
            @RequestParam(value = "query_priority", required = false) List<QueryPriority> priorities,
            @Parameter(description = "whether all the CIP specific properties should be returned in the response. False by default")
            @RequestParam(value = "show_cips_details", defaultValue = "false", required = false) Boolean showCipsDetails) {

        log.info("subject: {}, properties: {}, priorities: {}, showCipsDetails: {}",
                subject,
                properties != null ? String.join(",", properties) : "",
                priorities != null ? priorities.stream().map(QueryPriority::name).collect(Collectors.joining(",")) : "",
                showCipsDetails);

        List<String> queryProperties = properties != null ? properties : ALL_PROPERTIES;
        validateProperties(queryProperties);
        List<QueryPriority> queryPriority = priorities != null ? priorities : defaultQueryPriority;
        boolean includeCipsDetails = Boolean.TRUE.equals(showCipsDetails);

        Subject result = metadataV2QueryService.querySubject(subject, queryPriority, queryProperties, includeCipsDetails);

        if (result == null) {
            return ResponseEntity.notFound().build();
        }

        List<String> stringPriorities = queryPriority.stream().map(QueryPriority::name).toList();
        return ResponseEntity.ok(new Response(result, stringPriorities));
    }

    /**
     * Batch query metadata for multiple subjects.
     * <p>
     * CIP-113 data is pre-fetched in a single query to avoid N+1 lookups.
     * Subjects without valid metadata (missing {@code name} or {@code description}) are excluded.
     *
     * @param body            request body with list of subjects and optional property filter
     * @param priorities      optional CIP priority override
     * @param showCipsDetails whether to include raw per-standard metadata
     * @return list of merged subjects with valid metadata
     */
    @Operation(operationId = "getSubjects", summary = "Query either all or a subset of properties of the given subjects",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Batch results (subjects without valid metadata are excluded)")
            })
    @PostMapping(value = "/subjects/query", produces = {"application/json;charset=utf-8"}, consumes = {"application/json;charset=utf-8"})
    public ResponseEntity<BatchResponse> getSubjects(
            @Parameter(name = "body", required = true, schema = @Schema)
            @Valid @RequestBody BatchRequest body,
            @Parameter(description = "the CIP priority: if the same property is present in multiple standards, the one with highest priority is returned")
            @RequestParam(value = "query_priority", required = false) List<QueryPriority> priorities,
            @Parameter(description = "whether all the CIP specific properties should be returned in the response. False by default")
            @RequestParam(value = "show_cips_details", defaultValue = "false", required = false) Boolean showCipsDetails) {

        List<String> queryProperties = body.getProperties() != null ? body.getProperties() : ALL_PROPERTIES;
        validateProperties(queryProperties);
        List<QueryPriority> queryPriority = priorities != null ? priorities : defaultQueryPriority;
        boolean includeCipsDetails = Boolean.TRUE.equals(showCipsDetails);

        Map<String, ProgrammableTokenCip113> cip113Map = metadataV2QueryService.prefetchCip113(body.getSubjects());

        List<Subject> subjects = body.getSubjects()
                .stream()
                .map(subject -> metadataV2QueryService.querySubjectBatch(
                        subject, queryPriority, queryProperties, cip113Map, includeCipsDetails))
                .filter(s -> !s.metadata().isEmpty() && s.metadata().isValid())
                .toList();

        List<String> stringPriorities = queryPriority.stream().map(QueryPriority::name).toList();
        return ResponseEntity.ok(new BatchResponse(subjects, stringPriorities));
    }

    private void validateProperties(List<String> queryProperties) {
        if (!queryProperties.isEmpty() && !queryProperties.containsAll(REQUIRED_PROPERTIES)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "When filtering properties, 'name' and 'description' are required and must be included");
        }
    }

}

package com.bloxbean.cardano.yaci.store.extensions.assetstore.api.controller;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.QueryPriority;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.*;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.service.TokenQueryService;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.service.TokenQueryService.BatchPrefetchData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Subjects API — multi-standard token metadata with priority-based merging.
 * <p>
 * Merges metadata from multiple CIP standards into a single response:
 * <ul>
 *   <li><b>CIP-26</b> — offchain metadata synced from the GitHub cardano-token-registry</li>
 *   <li><b>CIP-68</b> — on-chain reference NFT metadata parsed from inline datums</li>
 *   <li><b>CIP-113</b> — programmable token extensions (transfer logic scripts, when enabled)</li>
 * </ul>
 *
 * <p><b>Query Priority:</b>
 * When the same property (e.g. {@code name}) exists in both CIP-26 and CIP-68, the standard
 * with higher priority wins. The default order is {@code CIP_68, CIP_26} (on-chain preferred),
 * configurable per-request via the {@code query_priority} parameter or globally via
 * {@code store.assets.default-query-priority}.
 *
 * @see TokenQueryService
 */
@RestController
@RequestMapping("${apiPrefix:/api/v1}")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "Token Metadata Subjects",
        description = "Multi-standard merged metadata API for token subjects")
@Slf4j
public class TokenMetadataSubjectController {

    private static final List<String> ALL_PROPERTIES = List.of();

    private static final List<String> REQUIRED_PROPERTIES = List.of("name", "description");

    private final TokenQueryService tokenQueryService;

    @Value("${store.assets.default-query-priority:CIP_68,CIP_26}")
    private List<QueryPriority> defaultQueryPriority;

    @Operation(operationId = "getSubject", summary = "Query either all or a subset of properties of a given subject",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Merged metadata found"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Required properties (name, description) missing from filter"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Subject not found or has no valid metadata")
            })
    @GetMapping(path = "/subject/{subject}", produces = {"application/json;charset=utf-8"})
    public ResponseEntity<SubjectResponse> getSubject(
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

        Subject result = tokenQueryService.querySubject(subject, queryPriority, queryProperties, includeCipsDetails);

        if (result == null) {
            return ResponseEntity.notFound().build();
        }

        List<String> stringPriorities = queryPriority.stream().map(QueryPriority::name).toList();
        return ResponseEntity.ok(new SubjectResponse(result, stringPriorities));
    }

    @Operation(operationId = "getSubjects", summary = "Query either all or a subset of properties of the given subjects",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Batch results (subjects without valid metadata are excluded)")
            })
    @PostMapping(value = "/subject/query", produces = {"application/json;charset=utf-8"}, consumes = {"application/json;charset=utf-8"})
    public ResponseEntity<SubjectBatchResponse> getSubjects(
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

        BatchPrefetchData prefetchData = tokenQueryService.prefetchBatch(body.getSubjects(), queryProperties);

        List<Subject> subjects = body.getSubjects()
                .stream()
                .map(subject -> tokenQueryService.querySubjectBatch(
                        subject, queryPriority, queryProperties, prefetchData, includeCipsDetails))
                .filter(s -> !s.metadata().isEmpty() && s.metadata().isValid())
                .toList();

        List<String> stringPriorities = queryPriority.stream().map(QueryPriority::name).toList();
        return ResponseEntity.ok(new SubjectBatchResponse(subjects, stringPriorities));
    }

    private void validateProperties(List<String> queryProperties) {
        if (!queryProperties.isEmpty() && !queryProperties.containsAll(REQUIRED_PROPERTIES)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "When filtering properties, 'name' and 'description' are required and must be included");
        }
    }

}

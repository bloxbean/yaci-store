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

@RestController
@RequestMapping("${store.extensions.asset-store.api-prefix:/api/v1}")
@ConditionalOnExpression("${store.extensions.asset-store.enabled:false}")
@RequiredArgsConstructor
@Slf4j
public class V2SubjectController {

    private static final List<String> ALL_PROPERTIES = List.of();

    private static final List<String> REQUIRED_PROPERTIES = List.of("name", "description");

    private final MetadataV2QueryService metadataV2QueryService;

    @Value("${store.extensions.asset-store.default-query-priority:CIP_68,CIP_26}")
    private List<QueryPriority> defaultQueryPriority;

    @Operation(operationId = "getSubject", summary = "Query either all or a subset of properties of a given subject")
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

    @Operation(operationId = "getSubjects", summary = "Query either all or a subset of properties of the given subjects")
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

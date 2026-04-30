package com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Batch subject query response.")
public record SubjectBatchResponse(
        @Schema(description = "List of subjects with valid metadata (subjects without name+description are excluded).")
        List<Subject> subjects,

        @Schema(description = "The CIP priority order that was used for this query.")
        List<String> queryPriority
) {}

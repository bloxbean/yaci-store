package com.bloxbean.cardano.yaci.store.api.governance.controller.local;

import com.bloxbean.cardano.yaci.store.governance.domain.local.LocalConstitution;
import com.bloxbean.cardano.yaci.store.governance.service.LocalGovStateServiceReader;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("${apiPrefix}/governance/live/constitution")
@RequiredArgsConstructor
@Tag(name = "Local Constitution Service", description = "Get constitution from local Cardano Node.")
@Slf4j
@ConditionalOnBean(LocalGovStateServiceReader.class)
@ConditionalOnExpression("${store.governance.endpoints.constitution.live.enabled:true}")
public class LocalConstitutionController {
    private final LocalGovStateServiceReader localGovStateServiceReader;

    @GetMapping
    @Operation(description = "Get current constitution in local node")
    public ResponseEntity<LocalConstitution> getCurrentConstitution() {

        return localGovStateServiceReader.getCurrentConstitution()
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Constitution not found"));
    }
}

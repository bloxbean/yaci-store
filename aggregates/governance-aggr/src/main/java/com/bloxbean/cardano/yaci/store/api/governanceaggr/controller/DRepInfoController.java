package com.bloxbean.cardano.yaci.store.api.governanceaggr.controller;

import com.bloxbean.cardano.yaci.store.api.governanceaggr.dto.DRepDetailsDto;
import com.bloxbean.cardano.yaci.store.api.governanceaggr.dto.SpecialDRepDto;
import com.bloxbean.cardano.yaci.store.api.governanceaggr.service.DRepApiService;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.bloxbean.cardano.yaci.store.common.util.ControllerPageUtil.adjustPage;

@RestController("DRepInfoController")
@RequestMapping("${apiPrefix}/governance-state")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "DRep API", description = "APIs for DRep related data.")
public class DRepInfoController {
    private final DRepApiService dRepApiService;

    @GetMapping("/dreps")
    public ResponseEntity<List<DRepDetailsDto>> getDReps(@RequestParam(required = false, defaultValue = "10") @Min(1) @Max(100) int count,
                                                         @RequestParam(required = false, defaultValue = "0") @Min(0) int page,
                                                         @RequestParam(required = false, defaultValue = "desc") Order order) {
        int p = adjustPage(page);

        List<DRepDetailsDto> dReps = dRepApiService.getDReps(p, count, order);
        return ResponseEntity.ok(dReps);
    }

    @GetMapping("/dreps/{drepId}")
    public ResponseEntity<DRepDetailsDto> getDRepDetails(@PathVariable String drepId) {
        return dRepApiService.getDRepDetailsByDRepId(drepId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/dreps/special")
    public ResponseEntity<List<SpecialDRepDto>> getSpecialDReps() {
        List<SpecialDRepDto> specialDReps = dRepApiService.getAutoAbstainAndNoConfidenceDRepDetail();
        return ResponseEntity.ok(specialDReps);
    }
}

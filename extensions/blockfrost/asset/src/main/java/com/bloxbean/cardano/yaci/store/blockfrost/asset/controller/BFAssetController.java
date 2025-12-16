package com.bloxbean.cardano.yaci.store.blockfrost.asset.controller;


import com.bloxbean.cardano.yaci.store.blockfrost.asset.dto.BFAssetDTO;
import com.bloxbean.cardano.yaci.store.blockfrost.asset.service.BFAssetService;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Blockfrost Assets")
@RequestMapping("${blockfrost.apiPrefix}/assets")
@ConditionalOnExpression("${store.extensions.blockfrost.asset.enabled:true}")
public class BFAssetController {

    private final BFAssetService bfAssetService;

    @PostConstruct
    public void postConstruct() {
        log.info("Blockfrost AssetController initialized >>>");
    }

    @GetMapping
    public List<BFAssetDTO> getAssets(@RequestParam(required = false, defaultValue = "100") @Min(1) @Max(100) int count,
                                      @RequestParam(required = false, defaultValue = "0") @Min(0) int page,
                                      @RequestParam(required = false, defaultValue = "asc") Order order) {
        //TODO -- Fix pagination index
        int p = page;
        if (p > 0)
            p = p - 1;

        return bfAssetService.getAssets(p, count, order);
    }

}


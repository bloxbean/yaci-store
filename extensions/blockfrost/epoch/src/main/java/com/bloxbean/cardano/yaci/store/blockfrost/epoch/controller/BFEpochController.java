package com.bloxbean.cardano.yaci.store.blockfrost.epoch.controller;

import com.bloxbean.cardano.yaci.store.api.epoch.service.EpochParamService;
import com.bloxbean.cardano.yaci.store.blockfrost.epoch.dto.BFEpochDto;
import com.bloxbean.cardano.yaci.store.blockfrost.epoch.dto.BFEpochStakeDto;
import com.bloxbean.cardano.yaci.store.blockfrost.epoch.dto.BFEpochStakePoolDto;
import com.bloxbean.cardano.yaci.store.blockfrost.epoch.dto.BFProtocolParamsDto;
import com.bloxbean.cardano.yaci.store.blockfrost.epoch.mapper.BFProtocolParamMapper;
import com.bloxbean.cardano.yaci.store.blockfrost.epoch.service.BFEpochService;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Blockfrost Epochs")
@RequestMapping("${blockfrost.apiPrefix}/epochs")
@ConditionalOnExpression("${store.extensions.blockfrost.epoch.enabled:false}")
public class BFEpochController {

    private final BFProtocolParamMapper bfProtocolParamMapper = BFProtocolParamMapper.INSTANCE;
    private final EpochParamService epochParamService;
    private final BFEpochService bfEpochService;



    @PostConstruct
    public void postConstruct() {
        log.info("Blockfrost EpochController initialized >>>");
    }

    @GetMapping("latest")
    @Operation(summary = "Latest epoch", description = "Return the information about the latest, therefore current, epoch.")
    public BFEpochDto getLatestEpoch() {
        return bfEpochService.getLatestEpoch();
    }

    @GetMapping("latest/parameters")
    @Operation(summary = "Latest epoch protocol parameters", description = "Return the protocol parameters for the latest epoch.")
    public BFProtocolParamsDto getLatestProtocolParams() {
        return epochParamService.getLatestProtocolParams()
                .map(bfProtocolParamMapper::toBFProtocolParamsDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "The requested component has not been found."));
    }

    @GetMapping("{number}/parameters")
    @Operation(summary = "Specific Epoch's Protocol Parameters", description = "Return the protocol parameters for the epoch specified.")
    public BFProtocolParamsDto getProtocolParamsByEpochNo(@PathVariable Integer number) {
        return epochParamService.getProtocolParams(number)
                .map(bfProtocolParamMapper::toBFProtocolParamsDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Protocol parameters not found for epoch: " + number));
    }

    @GetMapping("{number}")
    @Operation(summary = "Specific epoch", description = "Return the content of the requested epoch.")
    public BFEpochDto getEpochByNumber(@PathVariable Integer number) {
        return bfEpochService.getEpoch(number);
    }

    @GetMapping("{number}/next")
    @Operation(summary = "Listing of next epochs", description = "Return the list of epochs following a specific epoch.")
    public List<BFEpochDto> getNextEpochs(@PathVariable Integer number,
                                                 @RequestParam(required = false, defaultValue = "100") @Min(1) @Max(100) int count,
                                                 @RequestParam(required = false, defaultValue = "1") @Min(1) int page) {
        int p = page - 1;

        return bfEpochService.getNextEpochs(number, p, count);
    }

    @GetMapping("{number}/previous")
    @Operation(summary = "Listing of previous epochs", description = "Return the list of epochs preceding a specific epoch.")
    public List<BFEpochDto> getPreviousEpochs(@PathVariable Integer number,
                                                     @RequestParam(required = false, defaultValue = "100") @Min(1) @Max(100) int count,
                                                     @RequestParam(required = false, defaultValue = "1") @Min(1) int page) {
        int p = page - 1;

        return bfEpochService.getPreviousEpochs(number, p, count);
    }

    @GetMapping("{number}/stakes")
    @Operation(summary = "Stake distribution", description = "Return the active stake distribution for the specified epoch.")
    public List<BFEpochStakeDto> getEpochStakes(@PathVariable Integer number,
                                                @RequestParam(required = false, defaultValue = "100") @Min(1) @Max(100) int count,
                                                @RequestParam(required = false, defaultValue = "1") @Min(1) int page) {
        int p = page - 1;

        return bfEpochService.getEpochStakes(number, p, count);
    }

    @GetMapping("{number}/stakes/{pool_id}")
    @Operation(summary = "Stake distribution by pool", description = "Return the active stake distribution for the epoch specified by stake pool.")
    public List<BFEpochStakePoolDto> getEpochStakesByPool(@PathVariable Integer number,
                                                          @PathVariable("pool_id") String poolId,
                                                          @RequestParam(required = false, defaultValue = "100") @Min(1) @Max(100) int count,
                                                          @RequestParam(required = false, defaultValue = "1") @Min(1) int page) {
        int p = page - 1;

        return bfEpochService.getEpochStakesByPool(number, poolId, p, count);
    }

    @GetMapping("{number}/blocks")
    @Operation(summary = "Block distribution", description = "Return the blocks minted for the epoch specified.")
    public List<String> getEpochBlocks(@PathVariable Integer number,
                                       @RequestParam(required = false, defaultValue = "100") @Min(1) @Max(100) int count,
                                       @RequestParam(required = false, defaultValue = "1") @Min(1) int page,
                                       @RequestParam(required = false, defaultValue = "asc") Order order) {
        int p = page - 1;

        return bfEpochService.getEpochBlocks(number, p, count, order);
    }

    @GetMapping("{number}/blocks/{pool_id}")
    @Operation(summary = "Block distribution by pool", description = "Return the block minted for the epoch specified by stake pool.")
    public List<String> getEpochBlocksByPool(@PathVariable Integer number,
                                             @PathVariable("pool_id") String poolId,
                                             @RequestParam(required = false, defaultValue = "100") @Min(1) @Max(100) int count,
                                             @RequestParam(required = false, defaultValue = "1") @Min(1) int page,
                                             @RequestParam(required = false, defaultValue = "asc") Order order) {
        int p = page - 1;

        return bfEpochService.getEpochBlocksByPool(number, poolId, p, count, order);
    }
}

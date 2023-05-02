package com.bloxbean.cardano.yaci.store.blocks.controller;
import com.bloxbean.cardano.yaci.store.blocks.domain.Epoch;
import com.bloxbean.cardano.yaci.store.blocks.domain.EpochsPage;
import com.bloxbean.cardano.yaci.store.blocks.service.EpochService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController("EpochController")
@RequestMapping("${apiPrefix}/epochs")
@Slf4j
public class EpochController {

    private EpochService epochService;

    public EpochController(EpochService epochService) {
        this.epochService = epochService;
    }

    @GetMapping("{number}")
    @Operation(description = "Get epoch by number")
    public Epoch getEpochByNumber(@PathVariable int number) {
        return epochService.getEpochByNumber(number)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Epoch not found"));
    }

    @GetMapping
    @Operation(description = "Get epochs by page number and count")
    public EpochsPage getEpochs(@RequestParam(name = "page", defaultValue = "0") int page,
                                @RequestParam(name = "count", defaultValue = "10") int count) {
        //TODO -- Fix pagination index
        int p = page;
        if (p > 0)
            p = p - 1;
        return epochService.getEpochs(p, count);
    }

}

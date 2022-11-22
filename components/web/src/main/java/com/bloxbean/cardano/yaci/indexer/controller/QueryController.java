package com.bloxbean.cardano.yaci.indexer.controller;

import com.bloxbean.cardano.yaci.indexer.service.TipFinderService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/query")
public class QueryController {

    public TipFinderService tipFinderService;

    public QueryController(TipFinderService tipFinderService) {
        this.tipFinderService = tipFinderService;
    }

//    @GetMapping("/tip")
//    public Mono<Tip> getTip() {
//        return tipFinderService.getTip();
//    }
}

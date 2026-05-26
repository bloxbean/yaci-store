package com.bloxbean.cardano.yaci.store.blockfrost.scripts.service;

import com.bloxbean.cardano.yaci.store.blockfrost.scripts.dto.*;
import com.bloxbean.cardano.yaci.store.blockfrost.scripts.mapper.BFScriptsMapper;
import com.bloxbean.cardano.yaci.store.blockfrost.scripts.storage.BFScriptsStorageReader;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BFScriptsService {

    private final BFScriptsStorageReader storageReader;
    private final BFScriptsMapper mapper;

    public List<BFScriptListItemDto> getScripts(int page, int count, Order order) {
        return storageReader.getScripts(page, count, order).stream()
                .map(mapper::toListItemDto)
                .toList();
    }

    public BFScriptDto getScript(String scriptHash) {
        return storageReader.getScript(scriptHash)
                .map(mapper::toScriptDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "The requested component has not been found."));
    }

    public BFScriptJsonDto getScriptJson(String scriptHash) {
        return storageReader.getScript(scriptHash)
                .map(mapper::toScriptJsonDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "The requested component has not been found."));
    }

    public BFScriptCborDto getScriptCbor(String scriptHash) {
        return storageReader.getScript(scriptHash)
                .map(mapper::toScriptCborDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "The requested component has not been found."));
    }

    public List<BFScriptRedeemerDto> getScriptRedeemers(String scriptHash, int page, int count, Order order) {
        return storageReader.getScriptRedeemers(scriptHash, page, count, order).stream()
                .map(mapper::toRedeemerDto)
                .toList();
    }

    public BFDatumDto getDatum(String datumHash) {
        return storageReader.getDatum(datumHash)
                .map(mapper::toDatumDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "The requested component has not been found."));
    }

    public BFDatumCborDto getDatumCbor(String datumHash) {
        return storageReader.getDatum(datumHash)
                .map(mapper::toDatumCborDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "The requested component has not been found."));
    }
}

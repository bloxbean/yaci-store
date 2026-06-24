package com.bloxbean.cardano.yaci.store.blockfrost.metadata.service;

import com.bloxbean.cardano.yaci.store.api.metadata.service.MetadataService;
import com.bloxbean.cardano.yaci.store.blockfrost.metadata.dto.BFMetadataCborDto;
import com.bloxbean.cardano.yaci.store.blockfrost.metadata.dto.BFMetadataJsonDto;
import com.bloxbean.cardano.yaci.store.blockfrost.metadata.dto.BFMetadataLabelDto;
import com.bloxbean.cardano.yaci.store.blockfrost.metadata.mapper.BFMetadataMapper;
import com.bloxbean.cardano.yaci.store.blockfrost.metadata.storage.BFMetadataStorageReader;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.metadata.domain.TxMetadataLabel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class BFMetadataService {

    private final MetadataService metadataService;
    private final BFMetadataStorageReader bfMetadataStorageReader;
    private final BFMetadataMapper bfMetadataMapper;

    public List<BFMetadataLabelDto> getLabels(int page, int count, Order order) {
        return bfMetadataStorageReader.findLabelsWithCount(page, count, order);
    }


    public List<BFMetadataJsonDto> getMetadataByLabel(String label, int page, int count, Order order) {
        List<TxMetadataLabel> metadata = metadataService.getMetadataByLabel(label, page, count, order);
        if (metadata == null || metadata.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "The requested component has not been found.");
        }
        return metadata.stream()
                .map(bfMetadataMapper::toJsonDto)
                .toList();
    }


    public List<BFMetadataCborDto> getMetadataCborByLabel(String label, int page, int count, Order order) {
        List<TxMetadataLabel> metadata = metadataService.getMetadataByLabel(label, page, count, order);
        if (metadata == null || metadata.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "The requested component has not been found.");
        }
        return metadata.stream()
                .map(bfMetadataMapper::toCborDto)
                .toList();
    }
}

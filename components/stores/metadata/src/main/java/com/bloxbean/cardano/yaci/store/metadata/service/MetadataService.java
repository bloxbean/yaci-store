package com.bloxbean.cardano.yaci.store.metadata.service;

import com.bloxbean.cardano.client.util.JsonUtil;
import com.bloxbean.cardano.yaci.store.metadata.domain.TxMetadata;
import com.bloxbean.cardano.yaci.store.metadata.repository.TxMetadataLabelRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class MetadataService {
    private final TxMetadataLabelRepository metadataLabelRepository;

    public List<TxMetadata> getMetadataForTx(String txHash) {
        return metadataLabelRepository.findByTxHash(txHash)
                .stream().map(txMetadataLabel -> {
                    JsonNode jsonNode = null;

                    try {
                        jsonNode = JsonUtil.parseJson(txMetadataLabel.getBody());
                    } catch (Exception e) {
                        log.error("error parsing metadata : " + txMetadataLabel.getBody());
                    }

                    return TxMetadata.builder()
                            .label(txMetadataLabel.getLabel())
                            .body(jsonNode)
                            .build();
                }).collect(Collectors.toList());
    }
}

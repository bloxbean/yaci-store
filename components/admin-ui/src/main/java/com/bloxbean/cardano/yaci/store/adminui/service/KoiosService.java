package com.bloxbean.cardano.yaci.store.adminui.service;

import com.bloxbean.cardano.yaci.store.adminui.AdminUiProperties;
import com.bloxbean.cardano.yaci.store.adminui.dto.KoiosTotalsDto;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class KoiosService {
    private final StoreProperties storeProperties;
    private final AdminUiProperties adminUiProperties;
    private final RestTemplate adminUiRestTemplate;

    private static final long MAINNET_PROTOCOL_MAGIC = 764824073L;
    private static final long PREPROD_PROTOCOL_MAGIC = 1L;
    private static final long PREVIEW_PROTOCOL_MAGIC = 2L;

    /**
     * Check if Koios verification is available.
     * Returns true if:
     * 1. koiosVerificationEnabled is true in config
     * 2. The network is mainnet, preprod, or preview (not a custom network)
     */
    public boolean isVerificationAvailable() {
        if (!adminUiProperties.isKoiosVerificationEnabled()) {
            return false;
        }
        return getKoiosBaseUrl().isPresent();
    }

    public Optional<String> getKoiosBaseUrl() {
        long protocolMagic = storeProperties.getProtocolMagic();
        if (protocolMagic == MAINNET_PROTOCOL_MAGIC) {
            return Optional.of("https://api.koios.rest");
        } else if (protocolMagic == PREPROD_PROTOCOL_MAGIC) {
            return Optional.of("https://preprod.koios.rest");
        } else if (protocolMagic == PREVIEW_PROTOCOL_MAGIC) {
            return Optional.of("https://preview.koios.rest");
        }
        return Optional.empty();
    }

    public Optional<KoiosTotalsDto> getTotals(int epoch) {
        if (!adminUiProperties.isKoiosVerificationEnabled()) {
            log.debug("Koios verification is disabled via configuration");
            return Optional.empty();
        }

        Optional<String> baseUrlOpt = getKoiosBaseUrl();
        if (baseUrlOpt.isEmpty()) {
            log.debug("Koios verification not available for this network (protocol magic: {})",
                    storeProperties.getProtocolMagic());
            return Optional.empty();
        }

        String url = baseUrlOpt.get() + "/api/v1/totals?_epoch_no=" + epoch;
        log.debug("Fetching Koios totals from: {}", url);

        try {
            ResponseEntity<List<KoiosTotalsDto>> response = adminUiRestTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<KoiosTotalsDto>>() {}
            );

            List<KoiosTotalsDto> body = response.getBody();
            if (body != null && !body.isEmpty()) {
                return Optional.of(body.get(0));
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to fetch Koios totals for epoch {}: {}", epoch, e.getMessage());
            throw new RuntimeException("Failed to fetch data from Koios: " + e.getMessage(), e);
        }
    }
}

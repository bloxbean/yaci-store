package com.bloxbean.cardano.yaci.store.redis.app.storage;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.core.domain.Cursor;
import com.bloxbean.cardano.yaci.store.core.service.CursorService;
import com.bloxbean.cardano.yaci.store.extensions.redis.utxo.impl.RedisUtxoStorage;
import com.bloxbean.cardano.yaci.store.extensions.redis.utxo.impl.repository.RedisUtxoRepository;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.UtxoCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * This custom storage optional class will only keep UTxOs that contain specific
 * DEX's liquidity pools information such as specific LP Tokens (Minswap, Muesliswap, WingRiders)
 * or specific Payment Address (Sundaeswap use case)
 * This use case is used by <a href="https://github.com/adabox-aio/dextreme-sdk">Adabox Dextreme</a>
 * to allow dex aggregation capabilities.
 */
@Slf4j
@Component
public class CustomUtxoStorage extends RedisUtxoStorage {

    private final Set<String> factoryTokens = Set.of("13aa2accf2e1561723aa26871e071fdf32c867cff7e7d50ad470d62f4d494e53574150",
            "de9b756719341e79785aa13c164e7fe68c189ed04d61c9876b2fe53f4d7565736c69537761705f414d4d", "026a18d04a0c642759bb3d83b12e3344894e5c1c7b2aeb1a2113a5704c");
    private final Set<String> poolAddresses = Set.of("addr1w9qzpelu9hn45pefc0xr4ac4kdxeswq7pndul2vuj59u8tqaxdznu");
    private final CursorService cursorService;

    public CustomUtxoStorage(RedisUtxoRepository redisUtxoRepository, UtxoCache utxoCache, CursorService cursorService) {
        super(redisUtxoRepository, utxoCache);
        this.cursorService = cursorService;
    }

    @Scheduled(fixedDelay = 20, timeUnit = TimeUnit.SECONDS)
    private void cleanup() {
        Optional<Cursor> optionalCursor = cursorService.getCursor();
        if (optionalCursor.isPresent()) {
            Cursor cursor = optionalCursor.get();
            log.info("Current Block: {}", cursor.getBlock());
            if (cursor.getBlock() - 2160L > 0) {
                log.info("Deleted Spent UTxOs: {}", deleteBySpentAndBlockLessThan(cursor.getBlock() - 2160L));
            }
        }
    }

    @Override
    public void saveUnspent(List<AddressUtxo> addressUtxoList) {
        List<AddressUtxo> filteredAddressUtxos = addressUtxoList.stream()
                .filter(addressUtxo -> (containsFactoryToken(addressUtxo) || isPoolAddressUTxO(addressUtxo)))
                .toList();

        super.saveUnspent(filteredAddressUtxos);
    }

    public boolean containsFactoryToken(AddressUtxo addressUtxo) {
        List<Amt> amounts = addressUtxo.getAmounts();
        if (amounts != null) {
            for (final Amt amount : amounts) {
                if (amount.getUnit() != null && factoryTokens.contains(amount.getUnit())) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isPoolAddressUTxO(AddressUtxo addressUtxo) {
        if (addressUtxo.getOwnerAddr() != null) {
            return poolAddresses.contains(addressUtxo.getOwnerAddr());
        }
        return false;
    }
}

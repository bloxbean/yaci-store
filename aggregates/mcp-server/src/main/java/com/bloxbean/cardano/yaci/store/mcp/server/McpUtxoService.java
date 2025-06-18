package com.bloxbean.cardano.yaci.store.mcp.server;

import com.bloxbean.cardano.yaci.store.api.utxo.service.AddressService;
import com.bloxbean.cardano.yaci.store.common.domain.Utxo;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.bloxbean.cardano.yaci.store.common.util.Bech32Prefixes.ADDR_VKEY_HASH_PREFIX;
import static com.bloxbean.cardano.yaci.store.common.util.Bech32Prefixes.STAKE_ADDR_PREFIX;

@Service
@RequiredArgsConstructor
public class McpUtxoService {
    private final AddressService addressService;

    @Tool(name = "utxos-by-address",
            description = "Get UTxOs for an address or address verification key hash (addr_vkh). If the address is a stake address, it will return UTXOs for all base addresses associated with the stake address")
    public List<Utxo> getUtxosByAddress(String address, int page, int count) {
        int p = page;
        if (p > 0)
            p = p - 1;

        if (address.startsWith(ADDR_VKEY_HASH_PREFIX)) { //By payment verification key hash
            return addressService.getUtxoByPaymentCredential(address, p, count, Order.desc);
        } else if (address.startsWith(STAKE_ADDR_PREFIX)) { //stake address
            return addressService.getUtxoByStakeAddress(address, p, count, Order.desc);
        } else { //By address
            return addressService.getUtxoByAddress(address, p, count, Order.desc);
        }
    }
}

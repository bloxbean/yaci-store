package com.bloxbean.cardano.yaci.store.mcp.server;

import com.bloxbean.cardano.yaci.store.api.utxo.service.AddressService;
import com.bloxbean.cardano.yaci.store.common.domain.Utxo;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.utxo.domain.AddressTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.bloxbean.cardano.yaci.store.common.util.Bech32Prefixes.ADDR_VKEY_HASH_PREFIX;
import static com.bloxbean.cardano.yaci.store.common.util.Bech32Prefixes.STAKE_ADDR_PREFIX;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = {"store.utxo.enabled", "store.mcp-server.tools.utxos.enabled"},
    havingValue = "true",
    matchIfMissing = true
)
public class McpUtxoService {
    private final AddressService addressService;

    @Tool(name = "utxos-by-address",
            description = "Get UTXOs for a Cardano address (addr1...), payment key hash (addr_vkh...), or stake address (stake1...). Returns unspent transaction outputs with ADA and token amounts. Page is 1-based.")
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

    @Tool(name = "utxos-by-address-and-asset",
            description = "Get UTXOs for a Cardano address filtered by a specific asset. The asset should be in format: policyId + assetName (hex). Returns UTXOs containing the specified asset. Page is 1-based.")
    public List<Utxo> getUtxosByAddressAndAsset(String address, String asset, int page, int count) {
        int p = page;
        if (p > 0)
            p = p - 1;

        return addressService.getUtxoByAddressAndAsset(address, asset, p, count, Order.desc);
    }

    @Tool(name = "utxos-by-payment-credential",
            description = "Get UTXOs for a payment credential (payment key hash). Accepts Cardano address (addr1...), payment key hash (addr_vkh...), or raw hex payment credential. Returns all UTXOs controlled by this payment credential. Page is 1-based.")
    public List<Utxo> getUtxosByPaymentCredential(String paymentCredential, int page, int count) {
        int p = page;
        if (p > 0)
            p = p - 1;

        return addressService.getUtxoByPaymentCredential(paymentCredential, p, count, Order.desc);
    }

    @Tool(name = "utxos-by-payment-credential-and-asset",
            description = "Get UTXOs for a payment credential filtered by a specific asset. The asset should be in format: policyId + assetName (hex). Useful for finding all UTXOs of a specific token across all addresses with the same payment credential. Page is 1-based.")
    public List<Utxo> getUtxosByPaymentCredentialAndAsset(String paymentCredential, String asset, int page, int count) {
        int p = page;
        if (p > 0)
            p = p - 1;

        return addressService.getUtxoByPaymentCredentialAndAsset(paymentCredential, asset, p, count, Order.desc);
    }

    @Tool(name = "utxos-by-stake-address",
            description = "Get all UTXOs associated with a stake address (stake1...). Returns UTXOs from all base addresses that delegate to this stake address. Useful for checking total balance across all addresses in a wallet. Page is 1-based.")
    public List<Utxo> getUtxosByStakeAddress(String stakeAddress, int page, int count) {
        int p = page;
        if (p > 0)
            p = p - 1;

        return addressService.getUtxoByStakeAddress(stakeAddress, p, count, Order.desc);
    }

    @Tool(name = "utxos-by-stake-address-and-asset",
            description = "Get UTXOs for a stake address filtered by a specific asset. The asset should be in format: policyId + assetName (hex). Returns UTXOs containing the specified asset across all addresses delegating to this stake address. Page is 1-based.")
    public List<Utxo> getUtxosByStakeAddressAndAsset(String stakeAddress, String asset, int page, int count) {
        int p = page;
        if (p > 0)
            p = p - 1;

        return addressService.getUtxoByStakeAddressAndAsset(stakeAddress, asset, p, count, Order.desc);
    }

    @Tool(name = "transactions-by-address",
            description = "Get transaction history for a Cardano address. Returns transactions that involve the specified address, including both inputs and outputs. Useful for address activity tracking. Page is 1-based.")
    public List<AddressTransaction> getTransactionsByAddress(String address, int page, int count) {
        int p = page;
        if (p > 0)
            p = p - 1;

        return addressService.getTransactionsByAddress(address, p, count, Order.desc);
    }
}

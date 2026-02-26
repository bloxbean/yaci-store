package com.bloxbean.cardano.yaci.store.blockfrost.transaction.storage;

import com.bloxbean.cardano.yaci.store.blockfrost.transaction.dto.*;

import java.util.List;
import java.util.Optional;

public interface BFTransactionStorageReader {

    Optional<BFTransactionDto> findTransactionByHash(String txHash);

    Optional<BFTxUtxosDto> findTxUtxos(String txHash);

    Optional<String> findTxCborHex(String txHash);

    List<BFTxRedeemerDto> findTxRedeemers(String txHash);

    List<BFTxStakeDto> findTxStakes(String txHash);

    List<BFTxDelegationDto> findTxDelegations(String txHash);

    List<BFTxWithdrawalDto> findTxWithdrawals(String txHash);

    List<BFTxPoolUpdateDto> findTxPoolUpdates(String txHash);

    List<BFTxPoolRetireDto> findTxPoolRetires(String txHash);

    /**
     * Returns output amounts for a transaction (excluding collateral returns).
     * Lovelace is aggregated (single entry). Non-lovelace tokens are returned
     * per-output (not summed), matching Blockfrost API behaviour where the same
     * token appearing in multiple outputs yields multiple entries in the list.
     */
    List<BFAmountDto> findTxOutputAmounts(String txHash);

    int countStakeRegistrations(String txHash);

    int countPoolRegistrations(String txHash);
}

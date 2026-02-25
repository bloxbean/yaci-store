package com.bloxbean.cardano.yaci.store.blockfrost.transaction.storage;

import com.bloxbean.cardano.yaci.store.blockfrost.transaction.dto.*;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
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
     * Returns aggregated output amounts for a transaction (excluding collateral returns).
     * Used internally for deposit calculation and for the main transaction endpoint.
     */
    Map<String, BigInteger> findTxOutputAmounts(String txHash);

    int countStakeRegistrations(String txHash);

    int countPoolRegistrations(String txHash);
}

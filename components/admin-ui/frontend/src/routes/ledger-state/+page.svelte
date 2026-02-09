<script lang="ts">
    import { onMount } from 'svelte';
    import { api, type LedgerStateStatus, type KoiosTotals } from '$lib/api/client';
    import PageTitle from '$lib/components/layout/PageTitle.svelte';
    import StatusBadge from '$lib/components/common/StatusBadge.svelte';
    import Spinner from '$lib/components/common/Spinner.svelte';
    import ErrorMessage from '$lib/components/common/ErrorMessage.svelte';

    let status: LedgerStateStatus | null = null;
    let loading = true;
    let error: string | null = null;

    // Koios verification availability (from backend config)
    let koiosVerificationEnabled = false;

    // Verification state - capture values at verification time
    let verifying = false;
    let verificationResult: {
        verifiedEpoch: number;
        localTreasury: string;
        localReserves: string;
        koiosTreasury: string;
        koiosReserves: string;
        treasuryMatch: boolean;
        reservesMatch: boolean;
        treasuryDiff: bigint;
        reservesDiff: bigint;
    } | null = null;
    let verificationError: string | null = null;

    async function fetchData() {
        try {
            const [ledgerState, verificationAvailable] = await Promise.all([
                api.getLedgerState(),
                api.isKoiosVerificationEnabled()
            ]);
            status = ledgerState;
            koiosVerificationEnabled = verificationAvailable;
            error = null;
        } catch (e) {
            error = e instanceof Error ? e.message : 'Failed to fetch ledger state';
        } finally {
            loading = false;
        }
    }

    async function verifyWithKoios() {
        if (!status || !koiosVerificationEnabled) return;

        // Capture current values before verification
        const epochToVerify = status.lastProcessedEpoch;
        const treasuryToVerify = status.treasury;
        const reservesToVerify = status.reserves;

        if (!epochToVerify || epochToVerify <= 0 || !treasuryToVerify || !reservesToVerify) return;

        verifying = true;
        verificationError = null;
        verificationResult = null;

        try {
            const koiosData = await api.getKoiosTotals(epochToVerify);

            const localTreasury = BigInt(treasuryToVerify);
            const localReserves = BigInt(reservesToVerify);
            const koiosTreasury = BigInt(koiosData.treasury);
            const koiosReserves = BigInt(koiosData.reserves);

            verificationResult = {
                verifiedEpoch: epochToVerify,
                localTreasury: treasuryToVerify,
                localReserves: reservesToVerify,
                koiosTreasury: koiosData.treasury,
                koiosReserves: koiosData.reserves,
                treasuryMatch: localTreasury === koiosTreasury,
                reservesMatch: localReserves === koiosReserves,
                treasuryDiff: localTreasury - koiosTreasury,
                reservesDiff: localReserves - koiosReserves
            };
        } catch (e) {
            verificationError = e instanceof Error ? e.message : 'Failed to verify with Koios';
        } finally {
            verifying = false;
        }
    }

    onMount(() => {
        fetchData();
        // No auto-refresh for ledger state - data changes infrequently (once per epoch)
    });

    function getStatusBadge(jobStatus: string | undefined): 'success' | 'error' | 'warning' | 'info' {
        switch (jobStatus) {
            case 'COMPLETED':
                return 'success';
            case 'STARTED':
                return 'warning';
            case 'NOT_STARTED':
                return 'info';
            case 'ERROR':
                return 'error';
            case 'MODULE_NOT_AVAILABLE':
                return 'info';
            default:
                return 'info';
        }
    }

    function formatLovelace(lovelace: string | null): string {
        if (!lovelace) return 'N/A';
        // Convert lovelace to ADA (1 ADA = 1,000,000 lovelace)
        const ada = BigInt(lovelace) / BigInt(1_000_000);
        return ada.toLocaleString('en-US') + ' ADA';
    }

    function formatLovelaceNumber(lovelace: string | null): string {
        if (!lovelace) return 'N/A';
        return BigInt(lovelace).toLocaleString('en-US') + ' lovelace';
    }

    function formatDiffAda(diff: bigint): string {
        const ada = diff / BigInt(1_000_000);
        const sign = diff > 0 ? '+' : '';
        return sign + ada.toLocaleString('en-US') + ' ADA';
    }

    function formatDiffLovelace(diff: bigint): string {
        const sign = diff > 0 ? '+' : '';
        const absDiff = diff < 0 ? -diff : diff;
        const lovelaceStr = sign + absDiff.toLocaleString('en-US') + ' lovelace';

        // If >= 1 ADA, also show ADA equivalent
        if (absDiff >= BigInt(1_000_000)) {
            const ada = Number(absDiff) / 1_000_000;
            return lovelaceStr + ` (~${ada.toLocaleString('en-US', { maximumFractionDigits: 2 })} ADA)`;
        }
        return lovelaceStr;
    }

    $: canVerify = koiosVerificationEnabled && status?.lastProcessedEpoch && status.lastProcessedEpoch > 0 && !status.lastJobError && (status.treasury || status.reserves);
</script>

<div class="mb-6">
    <PageTitle title="Ledger State" description="Monitor ledger state calculation and epoch processing" />
</div>

{#if loading}
    <div class="flex items-center justify-center h-64">
        <Spinner size="lg" />
    </div>
{:else if error}
    <ErrorMessage message={error} />
{:else if status}
    {#if !status.enabled}
        <div class="bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800 rounded-lg p-4 mb-6">
            <div class="flex items-start gap-3">
                <svg class="w-5 h-5 text-amber-500 mt-0.5 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
                    <path fill-rule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clip-rule="evenodd"/>
                </svg>
                <div>
                    <h3 class="text-sm font-medium text-amber-800 dark:text-amber-200">Ledger State Calculation Disabled</h3>
                    <p class="text-sm text-amber-700 dark:text-amber-300 mt-1">
                        Ledger state calculation is not enabled. To enable, please check if the following properties are set:
                    </p>
                    <div class="mt-2 text-sm text-amber-700 dark:text-amber-300 space-y-1">
                        <div><code class="bg-amber-100 dark:bg-amber-800 px-1 rounded">store.account.stake-address-balance-enabled=true</code></div>
                        <div><code class="bg-amber-100 dark:bg-amber-800 px-1 rounded">store.adapot.enabled=true</code></div>
                        <div><code class="bg-amber-100 dark:bg-amber-800 px-1 rounded">store.governance-aggr.enabled=true</code></div>
                    </div>
                </div>
            </div>
        </div>
    {/if}

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div class="bg-white dark:bg-slate-800 rounded-lg shadow-sm border border-gray-200 dark:border-slate-700 p-6 transition-colors">
            <h3 class="text-sm font-medium text-gray-700 dark:text-gray-300 mb-4">Epoch Status</h3>
            <div class="space-y-4">
                <div class="flex items-center justify-between">
                    <span class="text-sm text-gray-500 dark:text-gray-400">Current Epoch</span>
                    <span class="text-lg font-semibold text-gray-900 dark:text-white">{status.currentEpoch}</span>
                </div>
                <div class="flex items-center justify-between">
                    <span class="text-sm text-gray-500 dark:text-gray-400">Last Processed Epoch</span>
                    <span class="text-lg font-semibold text-gray-900 dark:text-white">
                        {status.lastProcessedEpoch > 0 ? status.lastProcessedEpoch : 'N/A'}
                    </span>
                </div>
                <div class="flex items-center justify-between">
                    <span class="text-sm text-gray-500 dark:text-gray-400">Epochs Behind</span>
                    <span class="text-lg font-semibold {status.currentEpoch - status.lastProcessedEpoch > 2 ? 'text-red-600 dark:text-red-400' : 'text-gray-900 dark:text-white'}">
                        {status.lastProcessedEpoch > 0 ? status.currentEpoch - status.lastProcessedEpoch : 'N/A'}
                    </span>
                </div>
            </div>
        </div>

        <div class="bg-white dark:bg-slate-800 rounded-lg shadow-sm border border-gray-200 dark:border-slate-700 p-6 transition-colors">
            <h3 class="text-sm font-medium text-gray-700 dark:text-gray-300 mb-4">Job Status</h3>
            <div class="space-y-4">
                <div class="flex items-center justify-between">
                    <span class="text-sm text-gray-500 dark:text-gray-400">Job Running</span>
                    {#if status.jobRunning}
                        <StatusBadge status="warning" text="Running" />
                    {:else}
                        <StatusBadge status="info" text="Idle" />
                    {/if}
                </div>
                <div class="flex items-center justify-between">
                    <span class="text-sm text-gray-500 dark:text-gray-400">Last Job Status</span>
                    <StatusBadge status={getStatusBadge(status.lastJobStatus)} text={status.lastJobStatus} />
                </div>
                {#if status.lastJobError}
                    <div>
                        <span class="text-sm text-gray-500 dark:text-gray-400">
                            Last Error {status.lastErrorEpoch ? `(Epoch ${status.lastErrorEpoch})` : ''}
                        </span>
                        <p class="mt-1 text-sm text-red-600 dark:text-red-400 break-all">{status.lastJobError}</p>
                    </div>
                {/if}
            </div>
        </div>
    </div>

    {#if status.lastProcessedEpoch > 0 && !status.lastJobError && (status.treasury || status.reserves)}
        <div class="mt-6 bg-white dark:bg-slate-800 rounded-lg shadow-sm border border-gray-200 dark:border-slate-700 p-6 transition-colors">
            <div class="flex items-center justify-between mb-4">
                <h3 class="text-sm font-medium text-gray-700 dark:text-gray-300">Ledger Balance (Epoch {status.lastProcessedEpoch})</h3>
                {#if canVerify}
                    <button
                        on:click={verifyWithKoios}
                        disabled={verifying}
                        class="inline-flex items-center gap-2 px-3 py-1.5 text-sm font-medium text-indigo-600 dark:text-indigo-400 bg-indigo-50 dark:bg-indigo-900/20 border border-indigo-200 dark:border-indigo-800 rounded-lg hover:bg-indigo-100 dark:hover:bg-indigo-900/40 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                    >
                        {#if verifying}
                            <svg class="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
                                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                            </svg>
                            Verifying...
                        {:else}
                            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>
                            </svg>
                            Verify with Koios
                        {/if}
                    </button>
                {/if}
            </div>
            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div class="flex items-center justify-between p-4 bg-emerald-50 dark:bg-emerald-900/20 rounded-lg border border-emerald-200 dark:border-emerald-800">
                    <div>
                        <span class="text-sm text-emerald-600 dark:text-emerald-400">Treasury</span>
                        <p class="text-lg font-semibold text-emerald-700 dark:text-emerald-300 mt-1 cursor-help" title={formatLovelaceNumber(status.treasury)}>
                            {formatLovelace(status.treasury)}
                        </p>
                    </div>
                    <svg class="w-8 h-8 text-emerald-500 dark:text-emerald-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
                    </svg>
                </div>
                <div class="flex items-center justify-between p-4 bg-blue-50 dark:bg-blue-900/20 rounded-lg border border-blue-200 dark:border-blue-800">
                    <div>
                        <span class="text-sm text-blue-600 dark:text-blue-400">Reserves</span>
                        <p class="text-lg font-semibold text-blue-700 dark:text-blue-300 mt-1 cursor-help" title={formatLovelaceNumber(status.reserves)}>
                            {formatLovelace(status.reserves)}
                        </p>
                    </div>
                    <svg class="w-8 h-8 text-blue-500 dark:text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"/>
                    </svg>
                </div>
            </div>

            {#if verificationError}
                <div class="mt-4 p-4 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg">
                    <div class="flex items-center gap-2 text-red-700 dark:text-red-300">
                        <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
                            <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd"/>
                        </svg>
                        <span class="font-medium">Verification Error</span>
                    </div>
                    <p class="mt-1 text-sm text-red-600 dark:text-red-400">{verificationError}</p>
                </div>
            {/if}

            {#if verificationResult}
                {@const allMatch = verificationResult.treasuryMatch && verificationResult.reservesMatch}
                <div class="mt-4 p-4 rounded-lg border {allMatch ? 'bg-green-50 dark:bg-green-900/20 border-green-200 dark:border-green-800' : 'bg-red-50 dark:bg-red-900/20 border-red-200 dark:border-red-800'}">
                    <div class="flex items-center justify-between">
                        <div class="flex items-center gap-2 {allMatch ? 'text-green-700 dark:text-green-300' : 'text-red-700 dark:text-red-300'}">
                            {#if allMatch}
                                <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
                                    <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"/>
                                </svg>
                                <span class="font-medium">Verification Passed (Koios)</span>
                            {:else}
                                <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
                                    <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd"/>
                                </svg>
                                <span class="font-medium">Verification Failed (Koios)</span>
                            {/if}
                        </div>
                        <span class="text-xs text-gray-500 dark:text-gray-400">Epoch {verificationResult.verifiedEpoch}</span>
                    </div>
                    <div class="mt-3 space-y-2 text-sm">
                        <div class="flex items-start gap-2">
                            {#if verificationResult.treasuryMatch}
                                <svg class="w-4 h-4 mt-0.5 text-green-600 dark:text-green-400 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
                                    <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
                                </svg>
                                <span class="text-green-700 dark:text-green-300">
                                    Treasury: {formatLovelace(verificationResult.koiosTreasury)}
                                    <span class="text-green-600 dark:text-green-400 font-medium ml-1">Match</span>
                                </span>
                            {:else}
                                <svg class="w-4 h-4 mt-0.5 text-red-600 dark:text-red-400 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
                                    <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd"/>
                                </svg>
                                <div class="text-red-700 dark:text-red-300">
                                    <div>Treasury: <span class="text-red-600 dark:text-red-400 font-medium">Mismatch</span></div>
                                    <div class="text-xs text-gray-500 dark:text-gray-400 mt-1 space-y-0.5">
                                        <div>Local: {formatLovelaceNumber(verificationResult.localTreasury)}</div>
                                        <div>Koios: {formatLovelaceNumber(verificationResult.koiosTreasury)}</div>
                                        <div class="font-medium text-red-600 dark:text-red-400">Diff: {formatDiffLovelace(verificationResult.treasuryDiff)}</div>
                                    </div>
                                </div>
                            {/if}
                        </div>
                        <div class="flex items-start gap-2">
                            {#if verificationResult.reservesMatch}
                                <svg class="w-4 h-4 mt-0.5 text-green-600 dark:text-green-400 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
                                    <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
                                </svg>
                                <span class="text-green-700 dark:text-green-300">
                                    Reserves: {formatLovelace(verificationResult.koiosReserves)}
                                    <span class="text-green-600 dark:text-green-400 font-medium ml-1">Match</span>
                                </span>
                            {:else}
                                <svg class="w-4 h-4 mt-0.5 text-red-600 dark:text-red-400 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
                                    <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd"/>
                                </svg>
                                <div class="text-red-700 dark:text-red-300">
                                    <div>Reserves: <span class="text-red-600 dark:text-red-400 font-medium">Mismatch</span></div>
                                    <div class="text-xs text-gray-500 dark:text-gray-400 mt-1 space-y-0.5">
                                        <div>Local: {formatLovelaceNumber(verificationResult.localReserves)}</div>
                                        <div>Koios: {formatLovelaceNumber(verificationResult.koiosReserves)}</div>
                                        <div class="font-medium text-red-600 dark:text-red-400">Diff: {formatDiffLovelace(verificationResult.reservesDiff)}</div>
                                    </div>
                                </div>
                            {/if}
                        </div>
                    </div>
                </div>
            {/if}
        </div>
    {/if}

    {#if status.lastJobStatus === 'MODULE_NOT_AVAILABLE'}
        <div class="mt-6 bg-blue-50 dark:bg-blue-900/30 border border-blue-200 dark:border-blue-800 rounded-lg p-4">
            <p class="text-sm text-blue-800 dark:text-blue-300">
                The AdaPot module is not available. Ledger state calculation requires the adapot-spring-boot-starter
                to be included in your application.
            </p>
        </div>
    {/if}
{/if}

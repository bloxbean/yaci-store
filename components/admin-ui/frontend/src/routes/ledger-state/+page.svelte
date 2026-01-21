<script lang="ts">
    import { onMount, onDestroy } from 'svelte';
    import { api, type LedgerStateStatus } from '$lib/api/client';
    import PageTitle from '$lib/components/layout/PageTitle.svelte';
    import StatusBadge from '$lib/components/common/StatusBadge.svelte';
    import Spinner from '$lib/components/common/Spinner.svelte';
    import ErrorMessage from '$lib/components/common/ErrorMessage.svelte';

    let status: LedgerStateStatus | null = null;
    let loading = true;
    let error: string | null = null;
    let interval: ReturnType<typeof setInterval>;

    async function fetchData() {
        try {
            status = await api.getLedgerState();
            error = null;
        } catch (e) {
            error = e instanceof Error ? e.message : 'Failed to fetch ledger state';
        } finally {
            loading = false;
        }
    }

    onMount(() => {
        fetchData();
        interval = setInterval(fetchData, 20000);  // 20 seconds (Cardano block time)
    });

    onDestroy(() => {
        if (interval) clearInterval(interval);
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
</script>

<PageTitle title="Ledger State" description="Monitor ledger state calculation and epoch processing" />

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
                        AdaPot ledger state calculation is not enabled. To enable it, set <code class="bg-amber-100 dark:bg-amber-800 px-1 rounded">store.adapot.enabled=true</code> in your configuration.
                    </p>
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

    {#if status.lastJobStatus === 'MODULE_NOT_AVAILABLE'}
        <div class="mt-6 bg-blue-50 dark:bg-blue-900/30 border border-blue-200 dark:border-blue-800 rounded-lg p-4">
            <p class="text-sm text-blue-800 dark:text-blue-300">
                The AdaPot module is not available. Ledger state calculation requires the adapot-spring-boot-starter
                to be included in your application.
            </p>
        </div>
    {/if}
{/if}

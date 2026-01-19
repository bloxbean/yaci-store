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
        interval = setInterval(fetchData, 60000);  // 60 seconds
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
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div class="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
            <h3 class="text-sm font-medium text-gray-700 mb-4">Epoch Status</h3>
            <div class="space-y-4">
                <div class="flex items-center justify-between">
                    <span class="text-sm text-gray-500">Current Epoch</span>
                    <span class="text-lg font-semibold text-gray-900">{status.currentEpoch}</span>
                </div>
                <div class="flex items-center justify-between">
                    <span class="text-sm text-gray-500">Last Processed Epoch</span>
                    <span class="text-lg font-semibold text-gray-900">
                        {status.lastProcessedEpoch > 0 ? status.lastProcessedEpoch : 'N/A'}
                    </span>
                </div>
                <div class="flex items-center justify-between">
                    <span class="text-sm text-gray-500">Epochs Behind</span>
                    <span class="text-lg font-semibold {status.currentEpoch - status.lastProcessedEpoch > 2 ? 'text-red-600' : 'text-gray-900'}">
                        {status.lastProcessedEpoch > 0 ? status.currentEpoch - status.lastProcessedEpoch : 'N/A'}
                    </span>
                </div>
            </div>
        </div>

        <div class="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
            <h3 class="text-sm font-medium text-gray-700 mb-4">Job Status</h3>
            <div class="space-y-4">
                <div class="flex items-center justify-between">
                    <span class="text-sm text-gray-500">Job Running</span>
                    {#if status.jobRunning}
                        <StatusBadge status="warning" text="Running" />
                    {:else}
                        <StatusBadge status="info" text="Idle" />
                    {/if}
                </div>
                <div class="flex items-center justify-between">
                    <span class="text-sm text-gray-500">Last Job Status</span>
                    <StatusBadge status={getStatusBadge(status.lastJobStatus)} text={status.lastJobStatus} />
                </div>
                {#if status.lastJobError}
                    <div>
                        <span class="text-sm text-gray-500">Last Error</span>
                        <p class="mt-1 text-sm text-red-600 break-all">{status.lastJobError}</p>
                    </div>
                {/if}
            </div>
        </div>
    </div>

    {#if status.lastJobStatus === 'MODULE_NOT_AVAILABLE'}
        <div class="mt-6 bg-blue-50 border border-blue-200 rounded-lg p-4">
            <p class="text-sm text-blue-800">
                The AdaPot module is not available. Ledger state calculation requires the adapot-spring-boot-starter
                to be included in your application.
            </p>
        </div>
    {/if}
{/if}

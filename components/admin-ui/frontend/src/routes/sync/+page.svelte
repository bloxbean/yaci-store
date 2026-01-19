<script lang="ts">
    import { onMount, onDestroy } from 'svelte';
    import { api, type SyncStatus, type HealthStatus } from '$lib/api/client';
    import PageTitle from '$lib/components/layout/PageTitle.svelte';
    import SyncProgress from '$lib/components/dashboard/SyncProgress.svelte';
    import SyncControls from '$lib/components/sync/SyncControls.svelte';
    import Spinner from '$lib/components/common/Spinner.svelte';
    import ErrorMessage from '$lib/components/common/ErrorMessage.svelte';

    let syncStatus: SyncStatus | null = null;
    let health: HealthStatus | null = null;
    let loading = true;
    let error: string | null = null;
    let interval: ReturnType<typeof setInterval>;

    async function fetchData() {
        try {
            const [syncData, healthData] = await Promise.all([
                api.getSyncStatus(),
                api.getHealth()
            ]);
            syncStatus = syncData;
            health = healthData;
            error = null;
        } catch (e) {
            error = e instanceof Error ? e.message : 'Failed to fetch data';
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
</script>

<PageTitle title="Sync Status" description="Monitor and control blockchain synchronization" />

{#if loading}
    <div class="flex items-center justify-center h-64">
        <Spinner size="lg" />
    </div>
{:else if error}
    <ErrorMessage message={error} />
{:else if syncStatus && health}
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
        <SyncProgress
            percentage={syncStatus.syncPercentage}
            isSynced={syncStatus.synced}
            currentBlock={syncStatus.block}
            networkBlock={syncStatus.networkBlock}
        />

        <SyncControls
            isConnected={health.connectionAlive}
            isScheduledToStop={health.scheduledToStop}
            onAction={fetchData}
        />
    </div>

    <div class="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
        <h3 class="text-sm font-medium text-gray-700 mb-4">Sync Details</h3>
        <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
            <div>
                <p class="text-xs text-gray-500">Current Block</p>
                <p class="text-lg font-semibold text-gray-900">{syncStatus.block.toLocaleString()}</p>
            </div>
            <div>
                <p class="text-xs text-gray-500">Network Block</p>
                <p class="text-lg font-semibold text-gray-900">{syncStatus.networkBlock.toLocaleString()}</p>
            </div>
            <div>
                <p class="text-xs text-gray-500">Current Slot</p>
                <p class="text-lg font-semibold text-gray-900">{syncStatus.slot.toLocaleString()}</p>
            </div>
            <div>
                <p class="text-xs text-gray-500">Network Slot</p>
                <p class="text-lg font-semibold text-gray-900">{syncStatus.networkSlot.toLocaleString()}</p>
            </div>
            <div>
                <p class="text-xs text-gray-500">Epoch</p>
                <p class="text-lg font-semibold text-gray-900">{syncStatus.epoch}</p>
            </div>
            <div>
                <p class="text-xs text-gray-500">Era</p>
                <p class="text-lg font-semibold text-gray-900">{syncStatus.era}</p>
            </div>
            <div>
                <p class="text-xs text-gray-500">Sync Percentage</p>
                <p class="text-lg font-semibold text-gray-900">{syncStatus.syncPercentage.toFixed(4)}%</p>
            </div>
            <div>
                <p class="text-xs text-gray-500">Block Hash</p>
                <p class="text-xs font-mono text-gray-700 truncate" title={syncStatus.blockHash}>
                    {syncStatus.blockHash}
                </p>
            </div>
        </div>
    </div>
{/if}

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
    let controlEnabled = false;
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

    async function fetchControlEnabled() {
        try {
            controlEnabled = await api.getSyncControlEnabled();
        } catch (e) {
            // Default to false if we can't fetch the status
            controlEnabled = false;
        }
    }

    onMount(() => {
        fetchControlEnabled();
        fetchData();
        interval = setInterval(fetchData, 20000);  // 20 seconds (Cardano block time)
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
            {controlEnabled}
            onAction={fetchData}
        />
    </div>

    <div class="bg-white dark:bg-slate-800 rounded-lg shadow-sm border border-gray-200 dark:border-slate-700 p-6 transition-colors">
        <h3 class="text-sm font-medium text-gray-700 dark:text-gray-300 mb-4">Sync Details</h3>
        <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
            <div>
                <p class="text-xs text-gray-500 dark:text-gray-400">Current Block</p>
                <p class="text-lg font-semibold text-gray-900 dark:text-white">{syncStatus.block.toLocaleString()}</p>
            </div>
            <div>
                <p class="text-xs text-gray-500 dark:text-gray-400">Network Block</p>
                <p class="text-lg font-semibold text-gray-900 dark:text-white">{syncStatus.networkBlock.toLocaleString()}</p>
            </div>
            <div>
                <p class="text-xs text-gray-500 dark:text-gray-400">Current Slot</p>
                <p class="text-lg font-semibold text-gray-900 dark:text-white">{syncStatus.slot.toLocaleString()}</p>
            </div>
            <div>
                <p class="text-xs text-gray-500 dark:text-gray-400">Network Slot</p>
                <p class="text-lg font-semibold text-gray-900 dark:text-white">{syncStatus.networkSlot.toLocaleString()}</p>
            </div>
            <div>
                <p class="text-xs text-gray-500 dark:text-gray-400">Epoch</p>
                <p class="text-lg font-semibold text-gray-900 dark:text-white">{syncStatus.epoch}</p>
            </div>
            <div>
                <p class="text-xs text-gray-500 dark:text-gray-400">Era</p>
                <p class="text-lg font-semibold text-gray-900 dark:text-white">{syncStatus.era}</p>
            </div>
            <div>
                <p class="text-xs text-gray-500 dark:text-gray-400">Sync Percentage</p>
                <p class="text-lg font-semibold text-gray-900 dark:text-white">{syncStatus.syncPercentage.toFixed(4)}%</p>
            </div>
            <div>
                <p class="text-xs text-gray-500 dark:text-gray-400">Block Hash</p>
                <p class="text-xs font-mono text-gray-700 dark:text-gray-300 truncate" title={syncStatus.blockHash}>
                    {syncStatus.blockHash}
                </p>
            </div>
        </div>
    </div>

    <div class="mt-6 p-3 bg-blue-50 dark:bg-blue-900/30 border border-blue-200 dark:border-blue-800 rounded-lg">
        <p class="text-xs text-blue-700 dark:text-blue-300">
            <span class="font-medium">Note:</span> Network tip is refreshed every 3 minutes (when synced) or 15 minutes (when syncing) to minimize load on the Cardano node. The displayed network block/slot may be slightly behind the actual tip.
        </p>
    </div>
{/if}

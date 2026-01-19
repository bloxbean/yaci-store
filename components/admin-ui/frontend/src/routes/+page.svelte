<script lang="ts">
    import { onMount, onDestroy } from 'svelte';
    import { api, type SyncStatus, type HealthStatus } from '$lib/api/client';
    import PageTitle from '$lib/components/layout/PageTitle.svelte';
    import StatCard from '$lib/components/dashboard/StatCard.svelte';
    import SyncProgress from '$lib/components/dashboard/SyncProgress.svelte';
    import HealthBadge from '$lib/components/dashboard/HealthBadge.svelte';
    import Spinner from '$lib/components/common/Spinner.svelte';

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

<PageTitle title="Dashboard" description="Overview of Yaci Store sync status and health" />

{#if loading}
    <div class="flex items-center justify-center h-64">
        <Spinner size="lg" />
    </div>
{:else if error}
    <div class="rounded-md bg-red-50 p-4">
        <p class="text-sm font-medium text-red-800">{error}</p>
    </div>
{:else if syncStatus && health}
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-6">
        <StatCard
            title="Current Block"
            value={syncStatus.block.toLocaleString()}
            subtitle="Network: {syncStatus.networkBlock.toLocaleString()}"
            icon="block"
        />
        <StatCard
            title="Current Slot"
            value={syncStatus.slot.toLocaleString()}
            subtitle="Network: {syncStatus.networkSlot.toLocaleString()}"
            icon="slot"
        />
        <StatCard
            title="Epoch"
            value={syncStatus.epoch}
            icon="epoch"
        />
        <StatCard
            title="Era"
            value={syncStatus.era}
            icon="era"
        />
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <SyncProgress
            percentage={syncStatus.syncPercentage}
            isSynced={syncStatus.synced}
            currentBlock={syncStatus.block}
            networkBlock={syncStatus.networkBlock}
        />
        <HealthBadge {health} />
    </div>
{/if}

<script lang="ts">
    import type { HealthStatus } from '$lib/api/client';

    export let health: HealthStatus;

    $: isHealthy = health.connectionAlive && health.receivingBlocks && !health.error;

    function formatTime(ms: number): string {
        if (ms === 0) return 'N/A';
        const seconds = Math.floor(ms / 1000);
        if (seconds < 60) return `${seconds}s ago`;
        const minutes = Math.floor(seconds / 60);
        if (minutes < 60) return `${minutes}m ago`;
        const hours = Math.floor(minutes / 60);
        return `${hours}h ago`;
    }
</script>

<div class="stat-card">
    <div class="flex items-center justify-between mb-4">
        <h3 class="text-sm font-medium text-gray-700 dark:text-gray-300">Health Status</h3>
        {#if isHealthy}
            <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400">
                Healthy
            </span>
        {:else}
            <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400">
                Unhealthy
            </span>
        {/if}
    </div>
    <div class="space-y-3">
        <div class="flex items-center justify-between text-sm">
            <span class="text-gray-500 dark:text-gray-400">Connection</span>
            <span class={health.connectionAlive ? 'text-green-600 dark:text-green-400' : 'text-red-600 dark:text-red-400'}>
                {health.connectionAlive ? 'Connected' : 'Disconnected'}
            </span>
        </div>
        <div class="flex items-center justify-between text-sm">
            <span class="text-gray-500 dark:text-gray-400">Receiving Blocks</span>
            <span class={health.receivingBlocks ? 'text-green-600 dark:text-green-400' : 'text-red-600 dark:text-red-400'}>
                {health.receivingBlocks ? 'Yes' : 'No'}
            </span>
        </div>
        <div class="flex items-center justify-between text-sm">
            <span class="text-gray-500 dark:text-gray-400">Last Block</span>
            <span class="text-gray-700 dark:text-gray-300">{formatTime(health.timeSinceLastBlock)}</span>
        </div>
        {#if health.scheduledToStop}
            <div class="flex items-center justify-between text-sm">
                <span class="text-gray-500 dark:text-gray-400">Status</span>
                <span class="text-yellow-600 dark:text-yellow-400">Scheduled to Stop</span>
            </div>
        {/if}
        {#if health.error}
            <div class="flex items-center justify-between text-sm">
                <span class="text-gray-500 dark:text-gray-400">Error</span>
                <span class="text-red-600 dark:text-red-400">Yes</span>
            </div>
        {/if}
    </div>
</div>

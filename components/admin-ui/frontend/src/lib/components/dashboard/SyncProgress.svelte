<script lang="ts">
    export let percentage: number;
    export let isSynced: boolean;
    export let currentBlock: number;
    export let networkBlock: number;

    $: progressPercentage = Math.min(100, Math.max(0, percentage));
</script>

<div class="stat-card">
    <div class="flex items-center justify-between mb-2">
        <h3 class="text-sm font-medium text-gray-700 dark:text-gray-300">Sync Progress</h3>
        {#if isSynced}
            <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400">
                Synced
            </span>
        {:else}
            <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400">
                Syncing
            </span>
        {/if}
    </div>
    <div class="w-full bg-gray-200 dark:bg-slate-600 rounded-full h-2.5 mb-2">
        <div
            class="h-2.5 rounded-full transition-all duration-300 {isSynced ? 'bg-green-500' : 'bg-yaci-500'}"
            style="width: {progressPercentage}%"
        ></div>
    </div>
    <div class="flex justify-between text-xs text-gray-500 dark:text-gray-400">
        <span>Block {currentBlock.toLocaleString()}</span>
        <span>{progressPercentage.toFixed(2)}%</span>
        <span>Network {networkBlock.toLocaleString()}</span>
    </div>
</div>

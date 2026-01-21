<script lang="ts">
    import { api } from '$lib/api/client';
    import Spinner from '$lib/components/common/Spinner.svelte';

    export let isConnected: boolean;
    export let isScheduledToStop: boolean;
    export let controlEnabled: boolean = false;
    export let onAction: () => void = () => {};

    let isLoading = false;
    let error: string | null = null;

    async function handleStart() {
        isLoading = true;
        error = null;
        try {
            await api.startSync();
            onAction();
        } catch (e) {
            error = e instanceof Error ? e.message : 'Failed to start sync';
        } finally {
            isLoading = false;
        }
    }

    async function handleStop() {
        isLoading = true;
        error = null;
        try {
            await api.stopSync();
            onAction();
        } catch (e) {
            error = e instanceof Error ? e.message : 'Failed to stop sync';
        } finally {
            isLoading = false;
        }
    }

    async function handleRestart() {
        isLoading = true;
        error = null;
        try {
            await api.restartSync();
            onAction();
        } catch (e) {
            error = e instanceof Error ? e.message : 'Failed to restart sync';
        } finally {
            isLoading = false;
        }
    }
</script>

<div class="bg-white dark:bg-slate-800 rounded-lg shadow-sm border border-gray-200 dark:border-slate-700 p-6 transition-colors">
    <h3 class="text-sm font-medium text-gray-700 dark:text-gray-300 mb-4">Sync Controls</h3>

    {#if !controlEnabled}
        <div class="p-3 bg-gray-50 dark:bg-slate-700 text-gray-600 dark:text-gray-300 text-sm rounded-lg">
            Sync controls are disabled. To enable, set <code class="bg-gray-200 dark:bg-slate-600 px-1 rounded">store.admin.ui.sync-control-enabled=true</code> in your configuration.
        </div>
    {:else}
        {#if error}
            <div class="mb-4 p-3 bg-red-50 dark:bg-red-900/30 text-red-700 dark:text-red-400 text-sm rounded-lg">
                {error}
            </div>
        {/if}

        <div class="flex flex-wrap gap-3">
            {#if !isConnected}
                <button
                    on:click={handleStart}
                    disabled={isLoading}
                    class="btn-primary flex items-center space-x-2 disabled:opacity-50"
                >
                    {#if isLoading}
                        <Spinner size="sm" />
                    {/if}
                    <span>Start Sync</span>
                </button>
            {:else}
                <button
                    on:click={handleStop}
                    disabled={isLoading || isScheduledToStop}
                    class="btn-danger flex items-center space-x-2 disabled:opacity-50"
                >
                    {#if isLoading}
                        <Spinner size="sm" />
                    {/if}
                    <span>{isScheduledToStop ? 'Stopping...' : 'Stop Sync'}</span>
                </button>
            {/if}

            <button
                on:click={handleRestart}
                disabled={isLoading}
                class="btn-secondary flex items-center space-x-2 disabled:opacity-50"
            >
                {#if isLoading}
                    <Spinner size="sm" />
                {/if}
                <span>Restart Sync</span>
            </button>
        </div>

        <p class="mt-4 text-xs text-gray-500 dark:text-gray-400">
            {#if isConnected}
                Sync is currently running. Stop or restart to modify the sync process.
            {:else}
                Sync is not running. Click Start to begin synchronization.
            {/if}
        </p>
    {/if}
</div>

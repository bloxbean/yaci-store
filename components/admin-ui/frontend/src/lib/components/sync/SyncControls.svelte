<script lang="ts">
    import { api } from '$lib/api/client';
    import Spinner from '$lib/components/common/Spinner.svelte';

    export let isConnected: boolean;
    export let isScheduledToStop: boolean;
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

<div class="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
    <h3 class="text-sm font-medium text-gray-700 mb-4">Sync Controls</h3>

    {#if error}
        <div class="mb-4 p-3 bg-red-50 text-red-700 text-sm rounded-lg">
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

    <p class="mt-4 text-xs text-gray-500">
        {#if isConnected}
            Sync is currently running. Stop or restart to modify the sync process.
        {:else}
            Sync is not running. Click Start to begin synchronization.
        {/if}
    </p>
</div>

<script lang="ts">
    import { onMount } from 'svelte';
    import { api, type IndexStatus } from '$lib/api/client';
    import PageTitle from '$lib/components/layout/PageTitle.svelte';
    import StatusBadge from '$lib/components/common/StatusBadge.svelte';
    import Spinner from '$lib/components/common/Spinner.svelte';
    import ErrorMessage from '$lib/components/common/ErrorMessage.svelte';

    let indexes: IndexStatus[] = [];
    let loading = true;
    let refreshing = false;
    let error: string | null = null;
    let filter: 'all' | 'exists' | 'missing' = 'all';

    $: filteredIndexes = indexes.filter(idx => {
        if (filter === 'all') return true;
        if (filter === 'exists') return idx.exists;
        return !idx.exists;
    });

    $: existsCount = indexes.filter(i => i.exists).length;
    $: missingCount = indexes.filter(i => !i.exists).length;

    async function handleRefresh() {
        refreshing = true;
        error = null;
        try {
            indexes = await api.refreshIndexes();
        } catch (e) {
            error = e instanceof Error ? e.message : 'Failed to refresh indexes';
        } finally {
            refreshing = false;
        }
    }

    onMount(async () => {
        try {
            indexes = await api.getIndexes();
        } catch (e) {
            error = e instanceof Error ? e.message : 'Failed to fetch indexes';
        } finally {
            loading = false;
        }
    });
</script>

<PageTitle title="Database Indexes" description="View database index status" />

{#if loading}
    <div class="flex items-center justify-center h-64">
        <Spinner size="lg" />
    </div>
{:else if error}
    <ErrorMessage message={error} />
{:else}
    <div class="mb-6 flex items-center justify-between">
        <div class="flex items-center space-x-4">
            <span class="text-sm text-gray-600 dark:text-gray-400">
                {existsCount} existing, {missingCount} missing
            </span>
            <button
                on:click={handleRefresh}
                disabled={refreshing}
                class="flex items-center space-x-1 px-3 py-1.5 text-sm rounded-lg bg-gray-100 dark:bg-slate-700 text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-slate-600 disabled:opacity-50 transition-colors"
                title="Refresh index status from database"
            >
                {#if refreshing}
                    <Spinner size="sm" />
                {:else}
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                    </svg>
                {/if}
                <span>Refresh</span>
            </button>
        </div>
        <div class="flex items-center space-x-2">
            <button
                on:click={() => filter = 'all'}
                class="px-3 py-1.5 text-sm rounded-lg transition-colors {filter === 'all' ? 'bg-yaci-600 text-white' : 'bg-gray-100 dark:bg-slate-700 text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-slate-600'}"
            >
                All
            </button>
            <button
                on:click={() => filter = 'exists'}
                class="px-3 py-1.5 text-sm rounded-lg transition-colors {filter === 'exists' ? 'bg-green-600 text-white' : 'bg-gray-100 dark:bg-slate-700 text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-slate-600'}"
            >
                Existing
            </button>
            <button
                on:click={() => filter = 'missing'}
                class="px-3 py-1.5 text-sm rounded-lg transition-colors {filter === 'missing' ? 'bg-red-600 text-white' : 'bg-gray-100 dark:bg-slate-700 text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-slate-600'}"
            >
                Missing
            </button>
        </div>
    </div>

    {#if indexes.length === 0}
        <div class="bg-white dark:bg-slate-800 rounded-lg shadow-sm border border-gray-200 dark:border-slate-700 p-8 text-center transition-colors">
            <p class="text-gray-500 dark:text-gray-400">No indexes found. The dbutils module may not be available.</p>
        </div>
    {:else}
        <div class="bg-white dark:bg-slate-800 rounded-lg shadow-sm border border-gray-200 dark:border-slate-700 overflow-hidden transition-colors">
            <table class="min-w-full divide-y divide-gray-200 dark:divide-slate-700">
                <thead class="bg-gray-50 dark:bg-slate-700">
                    <tr>
                        <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                            Index Name
                        </th>
                        <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                            Table
                        </th>
                        <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                            Status
                        </th>
                    </tr>
                </thead>
                <tbody class="bg-white dark:bg-slate-800 divide-y divide-gray-200 dark:divide-slate-700">
                    {#each filteredIndexes as index}
                        <tr class="hover:bg-gray-50 dark:hover:bg-slate-700 transition-colors">
                            <td class="px-6 py-4 whitespace-nowrap">
                                <span class="text-sm font-mono text-gray-900 dark:text-gray-200">{index.name}</span>
                            </td>
                            <td class="px-6 py-4 whitespace-nowrap">
                                <span class="text-sm text-gray-600 dark:text-gray-400">{index.tableName}</span>
                            </td>
                            <td class="px-6 py-4 whitespace-nowrap">
                                <StatusBadge
                                    status={index.exists ? 'success' : 'error'}
                                    text={index.exists ? 'Exists' : 'Missing'}
                                />
                            </td>
                        </tr>
                    {/each}
                </tbody>
            </table>
        </div>
    {/if}
{/if}

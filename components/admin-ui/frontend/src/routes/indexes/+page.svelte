<script lang="ts">
    import { onMount } from 'svelte';
    import { api, type IndexStatus } from '$lib/api/client';
    import PageTitle from '$lib/components/layout/PageTitle.svelte';
    import StatusBadge from '$lib/components/common/StatusBadge.svelte';
    import Spinner from '$lib/components/common/Spinner.svelte';
    import ErrorMessage from '$lib/components/common/ErrorMessage.svelte';

    let indexes: IndexStatus[] = [];
    let loading = true;
    let error: string | null = null;
    let filter: 'all' | 'exists' | 'missing' = 'all';

    $: filteredIndexes = indexes.filter(idx => {
        if (filter === 'all') return true;
        if (filter === 'exists') return idx.exists;
        return !idx.exists;
    });

    $: existsCount = indexes.filter(i => i.exists).length;
    $: missingCount = indexes.filter(i => !i.exists).length;

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
            <span class="text-sm text-gray-600">
                {existsCount} existing, {missingCount} missing
            </span>
        </div>
        <div class="flex items-center space-x-2">
            <button
                on:click={() => filter = 'all'}
                class="px-3 py-1.5 text-sm rounded-lg transition-colors {filter === 'all' ? 'bg-yaci-600 text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'}"
            >
                All
            </button>
            <button
                on:click={() => filter = 'exists'}
                class="px-3 py-1.5 text-sm rounded-lg transition-colors {filter === 'exists' ? 'bg-green-600 text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'}"
            >
                Existing
            </button>
            <button
                on:click={() => filter = 'missing'}
                class="px-3 py-1.5 text-sm rounded-lg transition-colors {filter === 'missing' ? 'bg-red-600 text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'}"
            >
                Missing
            </button>
        </div>
    </div>

    {#if indexes.length === 0}
        <div class="bg-white rounded-lg shadow-sm border border-gray-200 p-8 text-center">
            <p class="text-gray-500">No indexes found. The dbutils module may not be available.</p>
        </div>
    {:else}
        <div class="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
            <table class="min-w-full divide-y divide-gray-200">
                <thead class="bg-gray-50">
                    <tr>
                        <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                            Index Name
                        </th>
                        <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                            Table
                        </th>
                        <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                            Status
                        </th>
                    </tr>
                </thead>
                <tbody class="bg-white divide-y divide-gray-200">
                    {#each filteredIndexes as index}
                        <tr class="hover:bg-gray-50">
                            <td class="px-6 py-4 whitespace-nowrap">
                                <span class="text-sm font-mono text-gray-900">{index.name}</span>
                            </td>
                            <td class="px-6 py-4 whitespace-nowrap">
                                <span class="text-sm text-gray-600">{index.tableName}</span>
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

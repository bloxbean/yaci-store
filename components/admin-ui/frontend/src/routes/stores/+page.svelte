<script lang="ts">
    import { onMount } from 'svelte';
    import { api, type StoreStatus } from '$lib/api/client';
    import PageTitle from '$lib/components/layout/PageTitle.svelte';
    import StoreCard from '$lib/components/stores/StoreCard.svelte';
    import Spinner from '$lib/components/common/Spinner.svelte';
    import ErrorMessage from '$lib/components/common/ErrorMessage.svelte';

    let stores: StoreStatus[] = [];
    let loading = true;
    let error: string | null = null;

    $: enabledCount = stores.filter(s => s.enabled).length;

    onMount(async () => {
        try {
            stores = await api.getStores();
        } catch (e) {
            error = e instanceof Error ? e.message : 'Failed to fetch stores';
        } finally {
            loading = false;
        }
    });
</script>

<PageTitle title="Stores" description="View and manage enabled data stores" />

{#if loading}
    <div class="flex items-center justify-center h-64">
        <Spinner size="lg" />
    </div>
{:else if error}
    <ErrorMessage message={error} />
{:else}
    <div class="mb-6">
        <p class="text-sm text-gray-600 dark:text-gray-400">
            {enabledCount} of {stores.length} stores enabled
        </p>
    </div>

    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
        {#each stores as store}
            <StoreCard {store} />
        {/each}
    </div>

    {#if stores.length === 0}
        <div class="text-center py-12 text-gray-500 dark:text-gray-400">
            <p>No stores found. Make sure the store starters are included in your application.</p>
        </div>
    {/if}
{/if}

<script lang="ts">
    import type { StoreStatus } from '$lib/api/client';
    import StatusBadge from '$lib/components/common/StatusBadge.svelte';

    export let store: StoreStatus;
</script>

<div class="bg-white rounded-lg shadow-sm border border-gray-200 p-4">
    <div class="flex items-center justify-between">
        <div>
            <h3 class="text-sm font-medium text-gray-900">{store.name}</h3>
            <div class="mt-2 flex items-center space-x-2">
                <StatusBadge status={store.enabled ? 'enabled' : 'disabled'} />
                {#if store.enabled && store.apiEnabled}
                    <StatusBadge status="info" text="API" />
                {/if}
            </div>
        </div>
        <div class="p-2 rounded-lg {store.enabled ? 'bg-green-50' : 'bg-gray-50'}">
            <svg class="w-5 h-5 {store.enabled ? 'text-green-600' : 'text-gray-400'}" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 7v10c0 2.21 3.582 4 8 4s8-1.79 8-4V7M4 7c0 2.21 3.582 4 8 4s8-1.79 8-4M4 7c0-2.21 3.582-4 8-4s8 1.79 8 4m0 5c0 2.21-3.582 4-8 4s-8-1.79-8-4" />
            </svg>
        </div>
    </div>
</div>

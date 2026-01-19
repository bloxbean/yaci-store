<script lang="ts">
    import { onMount } from 'svelte';
    import { api, type ConfigSection } from '$lib/api/client';
    import PageTitle from '$lib/components/layout/PageTitle.svelte';
    import Spinner from '$lib/components/common/Spinner.svelte';
    import ErrorMessage from '$lib/components/common/ErrorMessage.svelte';

    let sections: ConfigSection[] = [];
    let loading = true;
    let error: string | null = null;
    let expandedSections: Set<string> = new Set();

    onMount(async () => {
        try {
            sections = await api.getConfig();
            // Expand all sections by default
            expandedSections = new Set(sections.map(s => s.name));
        } catch (e) {
            error = e instanceof Error ? e.message : 'Failed to fetch configuration';
        } finally {
            loading = false;
        }
    });

    function toggleSection(name: string) {
        if (expandedSections.has(name)) {
            expandedSections.delete(name);
        } else {
            expandedSections.add(name);
        }
        expandedSections = expandedSections; // Trigger reactivity
    }

    function formatValue(value: unknown): string {
        if (value === null || value === undefined) {
            return '(not set)';
        }
        if (typeof value === 'boolean') {
            return value ? 'true' : 'false';
        }
        return String(value);
    }
</script>

<PageTitle title="Configuration" description="View current Yaci Store configuration" />

{#if loading}
    <div class="flex items-center justify-center h-64">
        <Spinner size="lg" />
    </div>
{:else if error}
    <ErrorMessage message={error} />
{:else}
    <div class="space-y-4">
        {#each sections as section}
            <div class="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
                <button
                    on:click={() => toggleSection(section.name)}
                    class="w-full px-6 py-4 flex items-center justify-between bg-gray-50 hover:bg-gray-100 transition-colors"
                >
                    <h3 class="text-sm font-medium text-gray-900">{section.name}</h3>
                    <svg
                        class="w-5 h-5 text-gray-500 transition-transform {expandedSections.has(section.name) ? 'rotate-180' : ''}"
                        fill="none"
                        stroke="currentColor"
                        viewBox="0 0 24 24"
                    >
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
                    </svg>
                </button>
                {#if expandedSections.has(section.name)}
                    <div class="px-6 py-4">
                        <table class="w-full">
                            <tbody class="divide-y divide-gray-100">
                                {#each Object.entries(section.properties) as [key, value]}
                                    <tr>
                                        <td class="py-2 pr-4 text-sm text-gray-500 w-1/3">{key}</td>
                                        <td class="py-2 text-sm font-mono {typeof value === 'boolean' ? (value ? 'text-green-600' : 'text-gray-500') : 'text-gray-900'}">
                                            {formatValue(value)}
                                        </td>
                                    </tr>
                                {/each}
                            </tbody>
                        </table>
                    </div>
                {/if}
            </div>
        {/each}

        {#if sections.length === 0}
            <div class="text-center py-12 text-gray-500">
                <p>No configuration available.</p>
            </div>
        {/if}
    </div>
{/if}

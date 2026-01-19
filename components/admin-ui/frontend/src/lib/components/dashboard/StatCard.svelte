<script lang="ts">
    export let title: string;
    export let value: string | number;
    export let subtitle: string = '';
    export let icon: string = '';

    function getIconPath(iconName: string): string {
        const icons: Record<string, string> = {
            block: 'M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4',
            slot: 'M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z',
            epoch: 'M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z',
            era: 'M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z'
        };
        return icons[iconName] || icons.block;
    }
</script>

<div class="stat-card">
    <div class="flex items-center justify-between">
        <div>
            <p class="text-sm font-medium text-gray-500">{title}</p>
            <p class="mt-1 text-2xl font-semibold text-gray-900">{value}</p>
            {#if subtitle}
                <p class="mt-1 text-xs text-gray-400">{subtitle}</p>
            {/if}
        </div>
        {#if icon}
            <div class="p-3 bg-yaci-50 rounded-lg">
                <svg class="w-6 h-6 text-yaci-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d={getIconPath(icon)} />
                </svg>
            </div>
        {/if}
    </div>
</div>

<script lang="ts">
    import { onMount } from 'svelte';
    import Logo from '$lib/components/common/Logo.svelte';
    import ThemeToggle from '$lib/components/common/ThemeToggle.svelte';
    import { api } from '$lib/api/client';

    const DEFAULT_HEADER_TEXT = 'Yaci Store Admin (Beta)';
    let headerText = DEFAULT_HEADER_TEXT;

    onMount(async () => {
        try {
            const settings = await api.getUiSettings();
            headerText = settings.headerText || DEFAULT_HEADER_TEXT;
        } catch (error) {
            console.error('Failed to fetch UI settings:', error);
        }
    });
</script>

<header class="bg-white dark:bg-slate-800 border-b border-gray-200 dark:border-slate-700 px-6 py-4 flex items-center justify-between transition-colors">
    <div class="flex items-center space-x-3">
        <Logo size={28} />
        <h1 class="text-xl font-semibold text-gray-900 dark:text-white">{headerText}</h1>
    </div>
    <div class="flex items-center">
        <ThemeToggle />
    </div>
</header>

<script lang="ts">
    import { onMount } from 'svelte';
    import '../app.css';
    import Header from '$lib/components/layout/Header.svelte';
    import Sidebar from '$lib/components/layout/Sidebar.svelte';
    import { initializeTheme } from '$lib/stores/theme';

    onMount(() => {
        initializeTheme();
    });
</script>

<div class="min-h-screen flex flex-col bg-gray-50 dark:bg-slate-900 transition-colors">
    <Header />
    <div class="flex flex-1">
        <Sidebar />
        <main class="flex-1 p-6 bg-gray-50 dark:bg-slate-900 transition-colors">
            <slot />
        </main>
    </div>
</div>

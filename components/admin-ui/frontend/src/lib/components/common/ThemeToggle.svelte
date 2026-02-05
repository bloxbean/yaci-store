<script lang="ts">
    import { resolvedTheme, toggleTheme } from '$lib/stores/theme';
</script>

<button
    on:click={toggleTheme}
    class="p-2 rounded-lg text-gray-500 hover:bg-gray-100 dark:text-gray-400 dark:hover:bg-slate-700 transition-colors"
    title={$resolvedTheme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}
    aria-label={$resolvedTheme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}
>
    {#if $resolvedTheme === 'dark'}
        <!-- Sun icon -->
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z" />
        </svg>
    {:else}
        <!-- Moon icon -->
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z" />
        </svg>
    {/if}
</button>

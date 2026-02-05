<script lang="ts">
    export let title: string;
    export let description: string = '';
</script>

<div class="mb-6">
    <h2 class="text-2xl font-semibold text-gray-900 dark:text-white">{title}</h2>
    {#if description}
        <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">{description}</p>
    {/if}
</div>

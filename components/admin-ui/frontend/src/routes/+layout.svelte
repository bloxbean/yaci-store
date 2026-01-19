<script lang="ts">
    import '../app.css';
    import Header from '$lib/components/layout/Header.svelte';
    import Sidebar from '$lib/components/layout/Sidebar.svelte';
</script>

<div class="min-h-screen flex flex-col">
    <Header />
    <div class="flex flex-1">
        <Sidebar />
        <main class="flex-1 p-6 bg-gray-50">
            <slot />
        </main>
    </div>
</div>
